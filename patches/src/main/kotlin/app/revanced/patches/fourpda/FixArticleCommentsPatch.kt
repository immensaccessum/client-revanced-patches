package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21t
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction30t
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder
import com.android.tools.smali.smali.smaliFlexLexer
import com.android.tools.smali.smali.smaliParser
import com.android.tools.smali.smali.smaliTreeWalker
import org.antlr.runtime.CommonTokenStream
import org.antlr.runtime.TokenSource
import org.antlr.runtime.tree.CommonTreeNodeStream
import java.io.StringReader

private const val Q0_CLASS = "Lru/fourpda/client/Q0;"
private const val Y_CLASS = "Lru/fourpda/client/y;"
private const val HELPER_CLASS = "Lru/fourpda/client/ArticleCommentsHelper;"

private fun Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun Instruction.field(): FieldReference? =
    (this as? ReferenceInstruction)?.reference as? FieldReference

private fun assemble(smali: String): ClassDef {
    val reader = StringReader(smali)
    val lexer = smaliFlexLexer(reader, 15)
    val tokens = CommonTokenStream(lexer as TokenSource)
    val parser = smaliParser(tokens)
    val fileTree = parser.smali_file()
    if (lexer.numberOfSyntaxErrors > 0 || parser.numberOfSyntaxErrors > 0) {
        error("ошибка сборки ArticleCommentsHelper")
    }
    val walker = smaliTreeWalker(
        CommonTreeNodeStream(fileTree.tree).apply { tokenStream = tokens },
    )
    walker.setDexBuilder(DexBuilder(Opcodes.getDefault()))
    return walker.smali_file()
}

val fixArticleCommentsPatch = bytecodePatch(
    name = "Ремонт комментариев в статьях",
    description = "Исправляет отображение комментариев в статьях: предотвращает обрыв списка при удаленных комментариях и восстанавливает правильную древовидную вложенность ответов",
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        if (classDefs[HELPER_CLASS] == null) {
            classDefs += assemble(ARTICLE_COMMENTS_HELPER_SMALI)
        }

        val q0Class = classDefs[Q0_CLASS] ?: error("в apk нет Q0")
        val parseMethod = proxy(q0Class).mutableClass.methods.firstOrNull { method ->
            method.name == "H" && method.returnType == "Z"
        } ?: error("не найден метод Q0.H")

        val instructions = parseMethod.implementation!!.instructions

        // 1. Находим вызов sa[13] -> this.k.l(13)
        var l13Index = -1
        for (i in 0 until instructions.size - 2) {
            val inst = instructions[i]
            if (inst.opcode == Opcode.INVOKE_VIRTUAL) {
                val methodRef = inst.calledMethod()
                if (methodRef?.name == "l" && methodRef.definingClass == Y_CLASS) {
                    val prev = instructions.getOrNull(i - 1)
                    val lit = (prev as? NarrowLiteralInstruction)?.narrowLiteral
                    if (lit == 13 || lit == 0xd) {
                        l13Index = i
                        break
                    }
                }
            }
        }

        if (l13Index < 0) {
            error("Не удалось найти вызов k.l(13) в Q0.H()")
        }

        // 2. Находим переход if-eqz, ведущий к концу блока комментариев (:cond_e, где проверяется w0.i:Z)
        var condEBranchIndex = -1
        var condEBranch: BuilderInstruction21t? = null
        for (i in l13Index until minOf(l13Index + 40, instructions.size)) {
            val inst = instructions[i]
            if (inst.opcode == Opcode.IF_EQZ) {
                val branch = inst as? BuilderInstruction21t ?: continue
                val targetInst = branch.target.location.instruction
                val fieldRef = targetInst?.field()
                if (fieldRef?.name == "i" && fieldRef.type == "Z" && fieldRef.definingClass == "Lru/fourpda/client/w0;") {
                    condEBranch = branch
                    condEBranchIndex = i
                    break
                }
            }
        }

        if (condEBranch == null || condEBranchIndex < 0) {
            error("Не удалось найти целевой переход cond_e в Q0.H()")
        }

        // 3. Заменяем проверку if-eqz v0, :cond_e на безусловный переход goto/32 :cond_e.
        // Это аккуратно пропускает старый сломанный парсер комментариев 4PDA,
        // сохраняя всю инициализацию объектов выше и не нарушая верификацию байткода ART.
        parseMethod.replaceInstruction(
            condEBranchIndex,
            BuilderInstruction30t(Opcode.GOTO_32, condEBranch.target),
        )

        // 4. Находим место записи списка комментариев в поле this.M (iput-object v10, v1, Q0->M)
        val mFieldWrite = instructions.indexOfFirst { instruction ->
            val field = instruction.field()
            instruction.opcode == Opcode.IPUT_OBJECT &&
                field?.definingClass == Q0_CLASS &&
                field.name == "M"
        }
        if (mFieldWrite < 0) {
            error("Не найдена запись в поле Q0.M")
        }

        // 5. Подменяем результат парсинга на вызов нашего ArticleCommentsHelper перед сохранением в this.M
        parseMethod.replaceInstruction(
            mFieldWrite,
            """
            iget-object v0, v1, Lru/fourpda/client/w0;->k:Lru/fourpda/client/y;
            if-eqz v0, :cond_skip_custom_comments
            const/16 v10, 0xd
            invoke-virtual {v0, v10}, Lru/fourpda/client/y;->l(I)Lru/fourpda/client/y;
            move-result-object v0
            invoke-static {v1, v0}, $HELPER_CLASS->parseComments(Lru/fourpda/client/Q0;Lru/fourpda/client/y;)Ljava/util/Vector;
            move-result-object v10
            :cond_skip_custom_comments
            iput-object v10, v1, Lru/fourpda/client/Q0;->M:Ljava/util/List;
            """.trimIndent(),
        )
    }
}

private const val ARTICLE_COMMENTS_HELPER_SMALI = """
.class public Lru/fourpda/client/ArticleCommentsHelper;
.super Ljava/lang/Object;

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static parseComments(Lru/fourpda/client/Q0;Lru/fourpda/client/y;)Ljava/util/Vector;
    .locals 14
    # p0 = q0
    # p1 = array

    new-instance v0, Ljava/util/Vector;
    invoke-direct {v0}, Ljava/util/Vector;-><init>()V

    if-eqz p1, :cond_empty
    invoke-virtual {p1}, Lru/fourpda/client/y;->d()I
    move-result v1
    if-nez v1, :cond_start

    :cond_empty
    return-object v0

    :cond_start
    :try_start_0
    new-instance v2, Ljava/util/ArrayList;
    invoke-direct {v2, v1}, Ljava/util/ArrayList;-><init>(I)V

    const/4 v3, 0x0

    :goto_parse_loop
    if-ge v3, v1, :cond_parse_done

    invoke-virtual {p1, v3}, Lru/fourpda/client/y;->l(I)Lru/fourpda/client/y;
    move-result-object v4
    if-eqz v4, :cond_next_row

    invoke-virtual {v4}, Lru/fourpda/client/y;->d()I
    move-result v5
    const/4 v6, 0x6
    if-ge v5, v6, :cond_check_id
    goto :cond_next_row

    :cond_check_id
    const/4 v5, 0x0
    invoke-virtual {v4, v5}, Lru/fourpda/client/y;->m(I)Ljava/lang/Integer;
    move-result-object v5
    if-nez v5, :cond_get_id
    goto :cond_next_row

    :cond_get_id
    invoke-virtual {v5}, Ljava/lang/Integer;->intValue()I
    move-result v5
    if-nez v5, :cond_create_c
    goto :cond_next_row

    :cond_create_c
    new-instance v7, Lru/fourpda/client/Q0${'$'}n;
    invoke-direct {v7, p0}, Lru/fourpda/client/Q0${'$'}n;-><init>(Lru/fourpda/client/Q0;)V
    iput v5, v7, Lru/fourpda/client/Q0${'$'}n;->a:I

    const/4 v6, 0x2
    invoke-virtual {v4, v6}, Lru/fourpda/client/y;->m(I)Ljava/lang/Integer;
    move-result-object v6
    if-eqz v6, :cond_zero_flags
    invoke-virtual {v6}, Ljava/lang/Integer;->intValue()I
    move-result v6
    goto :cond_set_flags
    :cond_zero_flags
    const/4 v6, 0x0
    :cond_set_flags
    iput v6, v7, Lru/fourpda/client/Q0${'$'}n;->c:I

    const/4 v8, 0x1
    invoke-virtual {v4, v8}, Lru/fourpda/client/y;->m(I)Ljava/lang/Integer;
    move-result-object v8
    if-eqz v8, :cond_zero_ts
    invoke-virtual {v8}, Ljava/lang/Integer;->intValue()I
    move-result v8
    goto :cond_get_user
    :cond_zero_ts
    const/4 v8, 0x0

    :cond_get_user
    const/4 v9, 0x3
    invoke-virtual {v4, v9}, Lru/fourpda/client/y;->m(I)Ljava/lang/Integer;
    move-result-object v9
    if-eqz v9, :cond_zero_user
    invoke-virtual {v9}, Ljava/lang/Integer;->intValue()I
    move-result v9
    goto :cond_set_user
    :cond_zero_user
    const/4 v9, 0x0
    :cond_set_user
    iput v9, v7, Lru/fourpda/client/Q0${'$'}n;->e:I

    const/4 v9, 0x5
    invoke-virtual {v4, v9}, Lru/fourpda/client/y;->m(I)Ljava/lang/Integer;
    move-result-object v9
    if-eqz v9, :cond_zero_parent
    invoke-virtual {v9}, Ljava/lang/Integer;->intValue()I
    move-result v9
    goto :cond_set_parent
    :cond_zero_parent
    const/4 v9, 0x0
    :cond_set_parent
    iput v9, v7, Lru/fourpda/client/Q0${'$'}n;->g:I

    and-int/lit8 v6, v6, 0x2
    if-nez v6, :cond_del_comment

    const/4 v6, 0x0
    iput-boolean v6, v7, Lru/fourpda/client/Q0${'$'}n;->d:Z

    const/4 v6, 0x4
    invoke-virtual {v4, v6}, Lru/fourpda/client/y;->n(I)Ljava/lang/String;
    move-result-object v6
    if-eqz v6, :cond_empty_author
    invoke-static {v6}, Lru/fourpda/client/s1${'$'}h;->b(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v6
    goto :cond_set_author
    :cond_empty_author
    const-string v6, ""
    :cond_set_author
    iput-object v6, v7, Lru/fourpda/client/Q0${'$'}n;->f:Ljava/lang/String;

    const/4 v9, 0x0
    const/4 v10, 0x1
    invoke-static {v8, v9, v9, v9, v10}, Lru/fourpda/client/s1;->q(IZZZZ)Ljava/lang/String;
    move-result-object v6
    iput-object v6, v7, Lru/fourpda/client/Q0${'$'}n;->b:Ljava/lang/String;

    const/4 v6, 0x6
    invoke-virtual {v4, v6}, Lru/fourpda/client/y;->n(I)Ljava/lang/String;
    move-result-object v6
    if-eqz v6, :cond_empty_text
    invoke-static {v6}, Lru/fourpda/client/s1${'$'}h;->b(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v6
    goto :cond_set_text
    :cond_empty_text
    const-string v6, ""
    :cond_set_text
    iput-object v6, v7, Lru/fourpda/client/Q0${'$'}n;->h:Ljava/lang/String;
    goto :cond_get_karma

    :cond_del_comment
    const/4 v6, 0x1
    iput-boolean v6, v7, Lru/fourpda/client/Q0${'$'}n;->d:Z
    const-string v6, ""
    iput-object v6, v7, Lru/fourpda/client/Q0${'$'}n;->f:Ljava/lang/String;
    iput-object v6, v7, Lru/fourpda/client/Q0${'$'}n;->b:Ljava/lang/String;
    iput-object v6, v7, Lru/fourpda/client/Q0${'$'}n;->h:Ljava/lang/String;

    :cond_get_karma
    const/4 v6, 0x7
    invoke-virtual {v4, v6}, Lru/fourpda/client/y;->m(I)Ljava/lang/Integer;
    move-result-object v6
    if-eqz v6, :cond_zero_karma
    invoke-virtual {v6}, Ljava/lang/Integer;->intValue()I
    move-result v6
    goto :cond_set_karma
    :cond_zero_karma
    const/4 v6, 0x0
    :cond_set_karma
    iput v6, v7, Lru/fourpda/client/Q0${'$'}n;->k:I

    const/16 v6, 0x8
    invoke-virtual {v4, v6}, Lru/fourpda/client/y;->n(I)Ljava/lang/String;
    move-result-object v6
    if-eqz v6, :cond_empty_avatar
    goto :cond_set_avatar
    :cond_empty_avatar
    const-string v6, ""
    :cond_set_avatar
    iput-object v6, v7, Lru/fourpda/client/Q0${'$'}n;->l:Ljava/lang/String;

    invoke-virtual {v2, v7}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_next_row
    add-int/lit8 v3, v3, 0x1
    goto/16 :goto_parse_loop

    :cond_parse_done
    invoke-virtual {v2}, Ljava/util/ArrayList;->isEmpty()Z
    move-result v1
    if-eqz v1, :cond_prune_start
    return-object v0

    :cond_prune_start
    :goto_prune_outer
    const/4 v3, 0x0
    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I
    move-result v4
    add-int/lit8 v4, v4, -0x1

    :goto_prune_loop
    if-ltz v4, :cond_prune_check_changed

    invoke-virtual {v2, v4}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;
    move-result-object v5
    check-cast v5, Lru/fourpda/client/Q0${'$'}n;
    iget-boolean v6, v5, Lru/fourpda/client/Q0${'$'}n;->d:Z
    if-eqz v6, :cond_prune_next

    iget v5, v5, Lru/fourpda/client/Q0${'$'}n;->a:I
    const/4 v6, 0x0
    const/4 v7, 0x0
    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I
    move-result v8

    :goto_check_children
    if-ge v6, v8, :cond_children_checked
    invoke-virtual {v2, v6}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;
    move-result-object v9
    check-cast v9, Lru/fourpda/client/Q0${'$'}n;
    iget v9, v9, Lru/fourpda/client/Q0${'$'}n;->g:I
    if-ne v9, v5, :cond_next_child_check
    const/4 v7, 0x1
    goto :cond_children_checked
    :cond_next_child_check
    add-int/lit8 v6, v6, 0x1
    goto :goto_check_children

    :cond_children_checked
    if-nez v7, :cond_prune_next
    invoke-virtual {v2, v4}, Ljava/util/ArrayList;->remove(I)Ljava/lang/Object;
    const/4 v3, 0x1

    :cond_prune_next
    add-int/lit8 v4, v4, -0x1
    goto :goto_prune_loop

    :cond_prune_check_changed
    if-eqz v3, :cond_build_maps
    goto :goto_prune_outer

    :cond_build_maps
    new-instance v3, Ljava/util/HashMap;
    invoke-direct {v3}, Ljava/util/HashMap;-><init>()V

    const/4 v4, 0x0
    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I
    move-result v5
    :goto_fill_by_id
    if-ge v4, v5, :cond_fill_by_id_done
    invoke-virtual {v2, v4}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;
    move-result-object v6
    check-cast v6, Lru/fourpda/client/Q0${'$'}n;
    iget v7, v6, Lru/fourpda/client/Q0${'$'}n;->a:I
    invoke-static {v7}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v7
    invoke-virtual {v3, v7, v6}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    add-int/lit8 v4, v4, 0x1
    goto :goto_fill_by_id

    :cond_fill_by_id_done
    new-instance v4, Ljava/util/HashMap;
    invoke-direct {v4}, Ljava/util/HashMap;-><init>()V

    new-instance v5, Ljava/util/ArrayList;
    invoke-direct {v5}, Ljava/util/ArrayList;-><init>()V

    const/4 v6, 0x0
    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I
    move-result v7
    :goto_group_loop
    if-ge v6, v7, :cond_group_done
    invoke-virtual {v2, v6}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;
    move-result-object v8
    check-cast v8, Lru/fourpda/client/Q0${'$'}n;
    iget v9, v8, Lru/fourpda/client/Q0${'$'}n;->g:I
    if-lez v9, :cond_add_root
    iget v10, v8, Lru/fourpda/client/Q0${'$'}n;->a:I
    if-eq v9, v10, :cond_add_root

    invoke-static {v9}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v10
    invoke-virtual {v3, v10}, Ljava/util/HashMap;->containsKey(Ljava/lang/Object;)Z
    move-result v11
    if-eqz v11, :cond_add_root

    invoke-virtual {v4, v10}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v9
    check-cast v9, Ljava/util/ArrayList;
    if-nez v9, :cond_have_ch_list
    new-instance v9, Ljava/util/ArrayList;
    invoke-direct {v9}, Ljava/util/ArrayList;-><init>()V
    invoke-virtual {v4, v10, v9}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    :cond_have_ch_list
    invoke-virtual {v9, v8}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
    goto :cond_next_group

    :cond_add_root
    invoke-virtual {v5, v8}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    :cond_next_group
    add-int/lit8 v6, v6, 0x1
    goto :goto_group_loop

    :cond_group_done
    const/16 v6, 0xa
    :try_start_max_lvl
    sget-object v7, Lru/fourpda/client/k1${'$'}a;->t0:[Landroid/graphics/drawable/Drawable;
    if-eqz v7, :cond_have_max_lvl
    array-length v8, v7
    const/4 v9, 0x1
    if-le v8, v9, :cond_have_max_lvl
    array-length v7, v7
    add-int/lit8 v6, v7, -0x1
    :cond_have_max_lvl
    :try_end_max_lvl
    .catch Ljava/lang/Throwable; {:try_start_max_lvl .. :try_end_max_lvl} :catch_max_lvl

    :catch_max_lvl
    new-instance v7, Ljava/util/ArrayList;
    invoke-direct {v7}, Ljava/util/ArrayList;-><init>()V

    new-instance v8, Ljava/util/ArrayList;
    invoke-direct {v8}, Ljava/util/ArrayList;-><init>()V

    invoke-virtual {v5}, Ljava/util/ArrayList;->size()I
    move-result v9
    add-int/lit8 v9, v9, -0x1
    :goto_push_roots
    if-ltz v9, :cond_roots_pushed
    invoke-virtual {v5, v9}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;
    move-result-object v10
    invoke-virtual {v7, v10}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
    const/4 v10, 0x1
    invoke-static {v10}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v10
    invoke-virtual {v8, v10}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
    add-int/lit8 v9, v9, -0x1
    goto :goto_push_roots

    :cond_roots_pushed
    new-instance v9, Ljava/util/HashSet;
    invoke-direct {v9}, Ljava/util/HashSet;-><init>()V

    :goto_dfs_loop
    invoke-virtual {v7}, Ljava/util/ArrayList;->isEmpty()Z
    move-result v10
    if-nez v10, :cond_dfs_done

    invoke-virtual {v7}, Ljava/util/ArrayList;->size()I
    move-result v10
    add-int/lit8 v10, v10, -0x1
    invoke-virtual {v7, v10}, Ljava/util/ArrayList;->remove(I)Ljava/lang/Object;
    move-result-object v11
    check-cast v11, Lru/fourpda/client/Q0${'$'}n;

    invoke-virtual {v8, v10}, Ljava/util/ArrayList;->remove(I)Ljava/lang/Object;
    move-result-object v10
    check-cast v10, Ljava/lang/Integer;
    invoke-virtual {v10}, Ljava/lang/Integer;->intValue()I
    move-result v10

    iget v12, v11, Lru/fourpda/client/Q0${'$'}n;->a:I
    invoke-static {v12}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v12
    invoke-virtual {v9, v12}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z
    move-result v13
    if-eqz v13, :cond_goto_dfs_loop

    invoke-static {v10, v6}, Ljava/lang/Math;->min(II)I
    move-result v13
    iput v13, v11, Lru/fourpda/client/Q0${'$'}n;->i:I
    invoke-virtual {v0, v11}, Ljava/util/Vector;->add(Ljava/lang/Object;)Z

    invoke-virtual {v4, v12}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;
    move-result-object v11
    check-cast v11, Ljava/util/ArrayList;
    if-eqz v11, :cond_goto_dfs_loop

    invoke-virtual {v11}, Ljava/util/ArrayList;->size()I
    move-result v12
    add-int/lit8 v12, v12, -0x1
    add-int/lit8 v10, v10, 0x1
    invoke-static {v10}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v10
    :goto_push_children
    if-ltz v12, :cond_goto_dfs_loop
    invoke-virtual {v11, v12}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;
    move-result-object v13
    invoke-virtual {v7, v13}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
    invoke-virtual {v8, v10}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
    add-int/lit8 v12, v12, -0x1
    goto :goto_push_children

    :cond_goto_dfs_loop
    goto :goto_dfs_loop

    :cond_dfs_done
    const/4 v4, 0x0
    invoke-virtual {v2}, Ljava/util/ArrayList;->size()I
    move-result v5
    :goto_fallback
    if-ge v4, v5, :cond_all_done
    invoke-virtual {v2, v4}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;
    move-result-object v6
    check-cast v6, Lru/fourpda/client/Q0${'$'}n;
    iget v7, v6, Lru/fourpda/client/Q0${'$'}n;->a:I
    invoke-static {v7}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
    move-result-object v7
    invoke-virtual {v9, v7}, Ljava/util/HashSet;->contains(Ljava/lang/Object;)Z
    move-result v7
    if-nez v7, :cond_next_fallback
    iget-boolean v7, v6, Lru/fourpda/client/Q0${'$'}n;->d:Z
    if-nez v7, :cond_next_fallback
    const/4 v7, 0x1
    iput v7, v6, Lru/fourpda/client/Q0${'$'}n;->i:I
    invoke-virtual {v0, v6}, Ljava/util/Vector;->add(Ljava/lang/Object;)Z
    :cond_next_fallback
    add-int/lit8 v4, v4, 0x1
    goto :goto_fallback

    :cond_all_done
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_all_comments

    :catch_all_comments
    return-object v0
.end method
"""

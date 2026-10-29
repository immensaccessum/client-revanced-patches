package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
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

private const val M0_CLASS = "Lru/fourpda/client/M0;"
private const val W0_CLASS = "Lru/fourpda/client/w0;"
private const val A_T_CLASS = "Lru/fourpda/client/a\$t;"
private const val L0_M_CLASS = "Lru/fourpda/client/L0\$m;"
private const val M0_P_CLASS = "Lru/fourpda/client/M0\$p;"
private const val M0_V_CLASS = "Lru/fourpda/client/M0\$v;"
private const val M0_B_CLASS = "Lru/fourpda/client/M0\$b;"
private const val QMS_FIX_HELPER = "Lru/fourpda/client/QmsFixHelper;"

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
        error("ошибка сборки QmsFixHelper")
    }
    val walker = smaliTreeWalker(
        CommonTreeNodeStream(fileTree.tree).apply { tokenStream = tokens },
    )
    walker.setDexBuilder(DexBuilder(Opcodes.getDefault()))
    return walker.smali_file()
}

private val QMS_FIX_HELPER_SMALI = """
.class public Lru/fourpda/client/QmsFixHelper;
.super Ljava/lang/Object;

.field public static lastJumpTime:J

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static onJump()V
    .locals 2
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J
    move-result-wide v0
    sput-wide v0, Lru/fourpda/client/QmsFixHelper;->lastJumpTime:J
    return-void
.end method

.method public static shouldAutoScroll(Landroid/widget/AdapterView;)I
    .locals 5
    :try_start_0
    if-nez p0, :cond_check
    const/4 v0, 0x2
    return v0

    :cond_check
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J
    move-result-wide v0
    sget-wide v2, Lru/fourpda/client/QmsFixHelper;->lastJumpTime:J
    sub-long/2addr v0, v2
    const-wide/16 v2, 0xbb8
    cmp-long v4, v0, v2
    if-gez v4, :cond_check_pos
    const/4 v0, 0x0
    return v0

    :cond_check_pos
    invoke-virtual {p0}, Landroid/widget/AdapterView;->getCount()I
    move-result v0
    if-eqz v0, :cond_ret_default
    invoke-virtual {p0}, Landroid/widget/AdapterView;->getLastVisiblePosition()I
    move-result v1
    add-int/lit8 v0, v0, -0x2
    if-ge v1, v0, :cond_ret_default
    const/4 v0, 0x1
    return v0

    :cond_ret_default
    const/4 v0, 0x2
    return v0
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_all

    :catch_all
    const/4 v0, 0x2
    return v0
.end method

.method public static deleteMessageInPlace(Lru/fourpda/client/L0${'$'}m;)V
    .locals 6
    :try_start_0
    if-nez p0, :cond_check_m
    return-void

    :cond_check_m
    iget-object v0, p0, Lru/fourpda/client/L0${'$'}m;->j:Lru/fourpda/client/w0;
    instance-of v1, v0, Lru/fourpda/client/M0;
    if-nez v1, :cond_m0
    if-eqz v0, :cond_ret
    invoke-virtual {v0}, Lru/fourpda/client/w0;->z()V
    :cond_ret
    return-void

    :cond_m0
    check-cast v0, Lru/fourpda/client/M0;
    iget-object v1, v0, Lru/fourpda/client/M0;->P:Landroid/util/SparseArray;
    if-nez v1, :cond_find
    invoke-virtual {v0}, Lru/fourpda/client/w0;->z()V
    return-void

    :cond_find
    iget v4, p0, Lru/fourpda/client/a${'$'}t;->h:I
    invoke-virtual {v1, v4}, Landroid/util/SparseArray;->indexOfKey(I)I
    move-result v4
    if-gez v4, :cond_remove
    invoke-virtual {v0}, Lru/fourpda/client/w0;->z()V
    return-void

    :cond_remove
    invoke-virtual {v0}, Lru/fourpda/client/w0;->W()V
    iget v2, v0, Lru/fourpda/client/w0;->x:I
    iget v3, v0, Lru/fourpda/client/w0;->w:I

    invoke-virtual {v1, v4}, Landroid/util/SparseArray;->removeAt(I)V
    invoke-virtual {v0}, Lru/fourpda/client/M0;->q0()V

    const/4 v5, 0x1
    invoke-virtual {v0, v5}, Lru/fourpda/client/w0;->a0(Z)V

    if-ge v4, v2, :cond_restore
    add-int/lit8 v2, v2, -0x1

    :cond_restore
    const/4 v5, 0x0
    invoke-static {v5, v2}, Ljava/lang/Math;->max(II)I
    move-result v5
    invoke-virtual {v0, v5, v3}, Lru/fourpda/client/w0;->Y(II)V
    return-void
    :try_end_0
    .catch Ljava/lang/Throwable; {:try_start_0 .. :try_end_0} :catch_0

    :catch_0
    if-eqz p0, :cond_err_done
    iget-object p0, p0, Lru/fourpda/client/L0${'$'}m;->j:Lru/fourpda/client/w0;
    if-eqz p0, :cond_err_done
    invoke-virtual {p0}, Lru/fourpda/client/w0;->z()V
    :cond_err_done
    return-void
.end method
"""

val fixQmsBugsPatch = bytecodePatch(
    name = "Fix QMS bugs",
    description = "Fixes scroll reset when deleting messages (in-place removal without reloading conversation), prevents screen snapping down when jumping to search results, and prevents search results from being overwritten by chat messages.",
    use = true,
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        if (classDefs[QMS_FIX_HELPER] == null) {
            classDefs += assemble(QMS_FIX_HELPER_SMALI)
        }

        // --- Делаем необходимые поля и методы публичными для безопасного вызова ---
        val m0Class = proxy(classDefs[M0_CLASS] ?: error("в apk нет M0")).mutableClass
        val pField = m0Class.fields.firstOrNull { it.name == "P" } ?: error("не найдено поле M0.P")
        pField.accessFlags = (pField.accessFlags and AccessFlags.PRIVATE.value.inv()) or AccessFlags.PUBLIC.value
        val q0Method = m0Class.methods.firstOrNull { it.name == "q0" } ?: error("не найден метод M0.q0")
        q0Method.accessFlags = (q0Method.accessFlags and AccessFlags.PRIVATE.value.inv()) or AccessFlags.PUBLIC.value

        val w0Class = proxy(classDefs[W0_CLASS] ?: error("в apk нет w0")).mutableClass
        val xField = w0Class.fields.firstOrNull { it.name == "x" } ?: error("не найдено поле w0.x")
        xField.accessFlags = (xField.accessFlags and AccessFlags.PRIVATE.value.inv()) or AccessFlags.PUBLIC.value
        val wField = w0Class.fields.firstOrNull { it.name == "w" } ?: error("не найдено поле w0.w")
        wField.accessFlags = (wField.accessFlags and AccessFlags.PRIVATE.value.inv()) or AccessFlags.PUBLIC.value
        val wMethod = w0Class.methods.firstOrNull { it.name == "W" } ?: error("не найден метод w0.W")
        wMethod.accessFlags = (wMethod.accessFlags and AccessFlags.PRIVATE.value.inv()) or AccessFlags.PUBLIC.value
        val yMethod = w0Class.methods.firstOrNull { it.name == "Y" } ?: error("не найден метод w0.Y")
        yMethod.accessFlags = (yMethod.accessFlags and AccessFlags.PRIVATE.value.inv()) or AccessFlags.PUBLIC.value
        val a0Method = w0Class.methods.firstOrNull { it.name == "a0" } ?: error("не найден метод w0.a0")
        a0Method.accessFlags = (a0Method.accessFlags and AccessFlags.PRIVATE.value.inv()) or AccessFlags.PUBLIC.value

        val atClass = proxy(classDefs[A_T_CLASS] ?: error("в apk нет a\$t")).mutableClass
        val hField = atClass.fields.firstOrNull { it.name == "h" } ?: error("не найдено поле a\$t.h")
        hField.accessFlags = (hField.accessFlags and AccessFlags.PRIVATE.value.inv()) or AccessFlags.PUBLIC.value

        val l0mClass = proxy(classDefs[L0_M_CLASS] ?: error("в apk нет L0\$m")).mutableClass
        val jField = l0mClass.fields.firstOrNull { it.name == "j" } ?: error("не найдено поле L0\$m.j")
        jField.accessFlags = (jField.accessFlags and AccessFlags.PRIVATE.value.inv()) or AccessFlags.PUBLIC.value

        // --- 1. Удаление сообщения на месте в L0$m.i ---
        val iMethod = l0mClass.methods.firstOrNull { it.name == "i" } ?: error("не найден метод L0\$m.i")
        val zIdx = iMethod.implementation!!.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.INVOKE_VIRTUAL && instruction.calledMethod()?.name == "z"
        }
        if (zIdx < 0) {
            error("не найден вызов w0.z() в L0\$m.i")
        }
        iMethod.replaceInstruction(
            zIdx,
            "invoke-static {p0}, $QMS_FIX_HELPER->deleteMessageInPlace($L0_M_CLASS)V",
        )

        // --- 2. Защита от сброса экрана вниз при переходе к старому сообщению ---
        // 2A. В M0$p.run фиксируем время прыжка через QmsFixHelper.onJump()
        val m0pClass = proxy(classDefs[M0_P_CLASS] ?: error("в apk нет M0\$p")).mutableClass
        val runMethod = m0pClass.methods.firstOrNull { it.name == "run" } ?: error("не найден метод M0\$p.run")
        val xIdx = runMethod.implementation!!.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.INVOKE_VIRTUAL && instruction.calledMethod()?.name == "X"
        }
        if (xIdx < 0) {
            error("не найден вызов w0.X() в M0\$p.run")
        }
        runMethod.addInstructions(
            xIdx + 1,
            "invoke-static {}, $QMS_FIX_HELPER->onJump()V",
        )

        // 2B. В M0$v.i вычисляем shouldAutoScroll
        val m0vClass = proxy(classDefs[M0_V_CLASS] ?: error("в apk нет M0\$v")).mutableClass
        val viMethod = m0vClass.methods.firstOrNull { it.name == "i" } ?: error("не найден метод M0\$v.i")
        val setTranscriptIdx = viMethod.implementation!!.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.INVOKE_VIRTUAL && instruction.calledMethod()?.name == "setTranscriptMode"
        }
        if (setTranscriptIdx < 0) {
            error("не найден вызов setTranscriptMode в M0\$v.i")
        }
        val const2Idx = setTranscriptIdx - 1
        require((viMethod.implementation!!.instructions[const2Idx] as? NarrowLiteralInstruction)?.narrowLiteral == 2) {
            "перед setTranscriptMode ожидалась константа 2"
        }
        viMethod.addInstructions(
            const2Idx + 1,
            """
            invoke-static {p1}, $QMS_FIX_HELPER->shouldAutoScroll(Landroid/widget/AdapterView;)I
            move-result p2
            """.trimIndent(),
        )

        // --- 3. Защита экрана результатов поиска ---
        // 3A. В M0.getView отключаем догрузку 60 сообщений переписки, если E != null
        val getViewMethod = m0Class.methods.firstOrNull { it.name == "getView" } ?: error("не найден метод M0.getView")
        val const30Idx = getViewMethod.implementation!!.instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.CONST_16 && (instruction as? NarrowLiteralInstruction)?.narrowLiteral == 30
        }
        if (const30Idx < 0) {
            error("не найдена инструкция const/16 30 в M0.getView")
        }
        getViewMethod.addInstructions(
            const30Idx,
            """
            iget-object v0, p0, $M0_CLASS->E:Ljava/lang/String;
            if-eqz v0, :cond_search_skip
            return-object p2
            :cond_search_skip
            """.trimIndent(),
        )

        // 3B. В M0$b.b игнорируем live-события диалога на экране поиска
        val m0bClass = proxy(classDefs[M0_B_CLASS] ?: error("в apk нет M0\$b")).mutableClass
        val bMethod = m0bClass.methods.firstOrNull { it.name == "b" } ?: error("не найден метод M0\$b.b")
        bMethod.addInstructions(
            0,
            """
            iget-object v0, p0, $M0_B_CLASS->a:$M0_CLASS
            iget-object v0, v0, $M0_CLASS->E:Ljava/lang/String;
            if-eqz v0, :cond_search_event_skip
            sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
            return-object v0
            :cond_search_event_skip
            """.trimIndent(),
        )
    }
}

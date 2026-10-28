package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder
import com.android.tools.smali.smali.smaliFlexLexer
import com.android.tools.smali.smali.smaliParser
import com.android.tools.smali.smali.smaliTreeWalker
import org.antlr.runtime.CommonTokenStream
import org.antlr.runtime.TokenSource
import org.antlr.runtime.tree.CommonTreeNodeStream
import java.io.StringReader

private const val WARN_TEMPLATES = "Lru/fourpda/client/WarnTemplates;"
private const val WARN_DIALOG = "Lru/fourpda/client/x;"
private const val SETTINGS_PAGE = "Lru/fourpda/client/I0;"
private const val SETTINGS_MENU_CB = "Lru/fourpda/client/I0\$D;"

private fun Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun assembleWarnTemplates(smali: String): ClassDef {
    val reader = StringReader(smali)
    val lexer = smaliFlexLexer(reader, 15)
    val tokens = CommonTokenStream(lexer as TokenSource)
    val parser = smaliParser(tokens)
    val fileTree = parser.smali_file()
    if (lexer.numberOfSyntaxErrors > 0 || parser.numberOfSyntaxErrors > 0) {
        error("ошибка сборки WarnTemplates")
    }
    val walker = smaliTreeWalker(
        CommonTreeNodeStream(fileTree.tree).apply { tokenStream = tokens },
    )
    walker.setDexBuilder(DexBuilder(Opcodes.getDefault()))
    return walker.smali_file()
}

private const val WARN_TEMPLATES_SMALI = """
.class public Lru/fourpda/client/WarnTemplates;
.super Ljava/lang/Object;
.implements Landroid/view/View${'$'}OnClickListener;
.implements Landroid/view/View${'$'}OnLongClickListener;
.implements Lru/fourpda/client/B1${'$'}b;

.field public static lastServerLevels:[Lru/fourpda/client/x${'$'}i;
.field public act:Lru/fourpda/client/MainActivity;
.field public warnDlg:Lru/fourpda/client/x;
.field public editDlg:Lru/fourpda/client/C1;
.field public mode:I

.method public constructor <init>(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;I)V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Lru/fourpda/client/WarnTemplates;->act:Lru/fourpda/client/MainActivity;
    iput-object p2, p0, Lru/fourpda/client/WarnTemplates;->warnDlg:Lru/fourpda/client/x;
    iput p3, p0, Lru/fourpda/client/WarnTemplates;->mode:I
    return-void
.end method

.method public static isCustomEnabled(Landroid/content/Context;)Z
    .locals 3
    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    const-string v1, "rv_warn_use_custom"
    const/4 v2, 0x0
    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
    move-result v1
    if-eqz v1, :ret_false
    const-string v1, "rv_warn_custom_json"
    const-string v2, ""
    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result v0
    xor-int/lit8 v0, v0, 0x1
    return v0
    :ret_false
    return v2
.end method

.method public static setCustomEnabled(Landroid/content/Context;Z)V
    .locals 2
    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
    move-result-object v0
    const-string v1, "rv_warn_use_custom"
    invoke-interface {v0, v1, p1}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
    invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->commit()Z
    return-void
.end method

.method public static levelsToJsonArray([Lru/fourpda/client/x${'$'}i;)Lorg/json/JSONArray;
    .locals 12
    new-instance v0, Lorg/json/JSONArray;
    invoke-direct {v0}, Lorg/json/JSONArray;-><init>()V
    if-eqz p0, :done

    array-length v1, p0
    const/4 v2, 0x0
    :loop_lvl
    if-ge v2, v1, :done
    aget-object v3, p0, v2
    if-eqz v3, :next_lvl

    :try_start
    new-instance v4, Lorg/json/JSONObject;
    invoke-direct {v4}, Lorg/json/JSONObject;-><init>()V

    iget-object v5, v3, Lru/fourpda/client/x${'$'}i;->a:Ljava/lang/String;
    if-nez v5, :has_lvl_id
    const-string v5, ""
    :has_lvl_id
    const-string v6, "id"
    invoke-virtual {v4, v6, v5}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    iget-object v5, v3, Lru/fourpda/client/x${'$'}i;->b:Ljava/lang/String;
    if-nez v5, :has_lvl_name
    const-string v5, ""
    :has_lvl_name
    const-string v6, "name"
    invoke-virtual {v4, v6, v5}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    iget v5, v3, Lru/fourpda/client/x${'$'}i;->c:I
    const-string v6, "flags"
    invoke-virtual {v4, v6, v5}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    new-instance v5, Lorg/json/JSONArray;
    invoke-direct {v5}, Lorg/json/JSONArray;-><init>()V

    iget-object v3, v3, Lru/fourpda/client/x${'$'}i;->d:[Lru/fourpda/client/x${'$'}i${'$'}a;
    if-eqz v3, :put_tpls
    array-length v6, v3
    const/4 v7, 0x0
    :loop_tpl
    if-ge v7, v6, :put_tpls
    aget-object v8, v3, v7
    if-eqz v8, :next_tpl

    new-instance v9, Lorg/json/JSONObject;
    invoke-direct {v9}, Lorg/json/JSONObject;-><init>()V

    iget-object v10, v8, Lru/fourpda/client/x${'$'}i${'$'}a;->a:Ljava/lang/String;
    if-nez v10, :has_tpl_id
    const-string v10, ""
    :has_tpl_id
    const-string v11, "id"
    invoke-virtual {v9, v11, v10}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    iget-object v10, v8, Lru/fourpda/client/x${'$'}i${'$'}a;->b:Ljava/lang/String;
    if-nez v10, :has_tpl_name
    const-string v10, ""
    :has_tpl_name
    const-string v11, "name"
    invoke-virtual {v9, v11, v10}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    iget-object v10, v8, Lru/fourpda/client/x${'$'}i${'$'}a;->c:Ljava/lang/String;
    if-nez v10, :has_tpl_reason
    const-string v10, ""
    :has_tpl_reason
    const-string v11, "reason"
    invoke-virtual {v9, v11, v10}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    iget-object v10, v8, Lru/fourpda/client/x${'$'}i${'$'}a;->d:Ljava/lang/String;
    if-nez v10, :has_tpl_msg
    const-string v10, ""
    :has_tpl_msg
    const-string v11, "message"
    invoke-virtual {v9, v11, v10}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    iget v10, v8, Lru/fourpda/client/x${'$'}i${'$'}a;->e:I
    const-string v11, "flags"
    invoke-virtual {v9, v11, v10}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    iget v10, v8, Lru/fourpda/client/x${'$'}i${'$'}a;->f:I
    const-string v11, "premod"
    invoke-virtual {v9, v11, v10}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    iget v10, v8, Lru/fourpda/client/x${'$'}i${'$'}a;->g:I
    const-string v11, "ro"
    invoke-virtual {v9, v11, v10}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    invoke-virtual {v5, v9}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    :next_tpl
    add-int/lit8 v7, v7, 0x1
    goto :loop_tpl

    :put_tpls
    const-string v3, "templates"
    invoke-virtual {v4, v3, v5}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;
    invoke-virtual {v0, v4}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :next_lvl

    :next_lvl
    add-int/lit8 v2, v2, 0x1
    goto :loop_lvl

    :done
    return-object v0
.end method

.method public static parseLevels(Ljava/lang/String;)[Lru/fourpda/client/x${'$'}i;
    .locals 15
    const/4 v0, 0x0
    if-eqz p0, :ret_null
    invoke-virtual {p0}, Ljava/lang/String;->trim()Ljava/lang/String;
    move-result-object p0
    invoke-static {p0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result v1
    if-nez v1, :ret_null

    :try_start
    new-instance v1, Lorg/json/JSONArray;
    invoke-direct {v1, p0}, Lorg/json/JSONArray;-><init>(Ljava/lang/String;)V
    invoke-virtual {v1}, Lorg/json/JSONArray;->length()I
    move-result v2
    if-lez v2, :ret_null

    const/4 v3, 0x0
    const/4 v4, 0x0
    :count_loop
    if-ge v3, v2, :count_done
    invoke-virtual {v1, v3}, Lorg/json/JSONArray;->optJSONObject(I)Lorg/json/JSONObject;
    move-result-object v5
    if-eqz v5, :count_next
    const-string v6, "templates"
    invoke-virtual {v5, v6}, Lorg/json/JSONObject;->optJSONArray(Ljava/lang/String;)Lorg/json/JSONArray;
    move-result-object v5
    if-eqz v5, :count_next
    invoke-virtual {v5}, Lorg/json/JSONArray;->length()I
    move-result v5
    if-lez v5, :count_next
    add-int/lit8 v4, v4, 0x1
    :count_next
    add-int/lit8 v3, v3, 0x1
    goto :count_loop

    :count_done
    if-lez v4, :ret_null
    new-array v3, v4, [Lru/fourpda/client/x${'$'}i;
    const/4 v4, 0x0
    const/4 v5, 0x0

    :fill_lvl_loop
    if-ge v4, v2, :fill_done
    invoke-virtual {v1, v4}, Lorg/json/JSONArray;->optJSONObject(I)Lorg/json/JSONObject;
    move-result-object v6
    if-eqz v6, :fill_lvl_next
    const-string v7, "templates"
    invoke-virtual {v6, v7}, Lorg/json/JSONObject;->optJSONArray(Ljava/lang/String;)Lorg/json/JSONArray;
    move-result-object v7
    if-eqz v7, :fill_lvl_next
    invoke-virtual {v7}, Lorg/json/JSONArray;->length()I
    move-result v8
    if-lez v8, :fill_lvl_next

    new-instance v9, Lru/fourpda/client/x${'$'}i;
    invoke-direct {v9, v0}, Lru/fourpda/client/x${'$'}i;-><init>(Lru/fourpda/client/x${'$'}a;)V

    const-string v10, "id"
    const-string v11, ""
    invoke-virtual {v6, v10, v11}, Lorg/json/JSONObject;->optString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v10
    iput-object v10, v9, Lru/fourpda/client/x${'$'}i;->a:Ljava/lang/String;

    const-string v10, "name"
    const-string v11, "Уровень"
    invoke-virtual {v6, v10, v11}, Lorg/json/JSONObject;->optString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v10
    iput-object v10, v9, Lru/fourpda/client/x${'$'}i;->b:Ljava/lang/String;

    const-string v10, "flags"
    const/4 v11, 0x0
    invoke-virtual {v6, v10, v11}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;I)I
    move-result v6
    iput v6, v9, Lru/fourpda/client/x${'$'}i;->c:I

    new-array v6, v8, [Lru/fourpda/client/x${'$'}i${'$'}a;
    iput-object v6, v9, Lru/fourpda/client/x${'$'}i;->d:[Lru/fourpda/client/x${'$'}i${'$'}a;

    const/4 v10, 0x0
    :fill_tpl_loop
    if-ge v10, v8, :fill_tpl_done
    invoke-virtual {v7, v10}, Lorg/json/JSONArray;->optJSONObject(I)Lorg/json/JSONObject;
    move-result-object v11
    new-instance v12, Lru/fourpda/client/x${'$'}i${'$'}a;
    invoke-direct {v12, v0}, Lru/fourpda/client/x${'$'}i${'$'}a;-><init>(Lru/fourpda/client/x${'$'}a;)V

    if-eqz v11, :tpl_defaults
    const-string v13, "id"
    const-string v14, ""
    invoke-virtual {v11, v13, v14}, Lorg/json/JSONObject;->optString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v13
    iput-object v13, v12, Lru/fourpda/client/x${'$'}i${'$'}a;->a:Ljava/lang/String;

    const-string v13, "name"
    const-string v14, "Шаблон"
    invoke-virtual {v11, v13, v14}, Lorg/json/JSONObject;->optString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v13
    iput-object v13, v12, Lru/fourpda/client/x${'$'}i${'$'}a;->b:Ljava/lang/String;

    const-string v13, "reason"
    const-string v14, ""
    invoke-virtual {v11, v13, v14}, Lorg/json/JSONObject;->optString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v13
    iput-object v13, v12, Lru/fourpda/client/x${'$'}i${'$'}a;->c:Ljava/lang/String;

    const-string v13, "message"
    const-string v14, ""
    invoke-virtual {v11, v13, v14}, Lorg/json/JSONObject;->optString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v13
    iput-object v13, v12, Lru/fourpda/client/x${'$'}i${'$'}a;->d:Ljava/lang/String;

    const-string v13, "flags"
    const/4 v14, 0x0
    invoke-virtual {v11, v13, v14}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;I)I
    move-result v13
    iput v13, v12, Lru/fourpda/client/x${'$'}i${'$'}a;->e:I

    const-string v13, "premod"
    invoke-virtual {v11, v13, v14}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;I)I
    move-result v13
    iput v13, v12, Lru/fourpda/client/x${'$'}i${'$'}a;->f:I

    const-string v13, "ro"
    invoke-virtual {v11, v13, v14}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;I)I
    move-result v11
    iput v11, v12, Lru/fourpda/client/x${'$'}i${'$'}a;->g:I
    goto :store_tpl

    :tpl_defaults
    const-string v13, ""
    iput-object v13, v12, Lru/fourpda/client/x${'$'}i${'$'}a;->a:Ljava/lang/String;
    iput-object v13, v12, Lru/fourpda/client/x${'$'}i${'$'}a;->b:Ljava/lang/String;
    iput-object v13, v12, Lru/fourpda/client/x${'$'}i${'$'}a;->c:Ljava/lang/String;
    iput-object v13, v12, Lru/fourpda/client/x${'$'}i${'$'}a;->d:Ljava/lang/String;

    :store_tpl
    aput-object v12, v6, v10
    add-int/lit8 v10, v10, 0x1
    goto :fill_tpl_loop

    :fill_tpl_done
    aput-object v9, v3, v5
    add-int/lit8 v5, v5, 0x1

    :fill_lvl_next
    add-int/lit8 v4, v4, 0x1
    goto :fill_lvl_loop

    :fill_done
    return-object v3
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :ret_null

    :ret_null
    return-object v0
.end method

.method public static mergeJsonArrays(Lorg/json/JSONArray;Lorg/json/JSONArray;)Lorg/json/JSONArray;
    .locals 14
    if-nez p0, :check_add
    new-instance p0, Lorg/json/JSONArray;
    invoke-direct {p0}, Lorg/json/JSONArray;-><init>()V
    :check_add
    if-eqz p1, :done

    invoke-virtual {p1}, Lorg/json/JSONArray;->length()I
    move-result v0
    const/4 v1, 0x0

    :loop_add_lvl
    if-ge v1, v0, :done
    invoke-virtual {p1, v1}, Lorg/json/JSONArray;->optJSONObject(I)Lorg/json/JSONObject;
    move-result-object v2
    if-eqz v2, :next_add_lvl

    const-string v3, "id"
    invoke-virtual {v2, v3}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v4
    const-string v5, "name"
    invoke-virtual {v2, v5}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v6

    invoke-virtual {p0}, Lorg/json/JSONArray;->length()I
    move-result v7
    const/4 v8, 0x0
    const/4 v9, 0x0

    :find_lvl
    if-ge v8, v7, :lvl_checked
    invoke-virtual {p0, v8}, Lorg/json/JSONArray;->optJSONObject(I)Lorg/json/JSONObject;
    move-result-object v10
    if-eqz v10, :find_lvl_next
    invoke-virtual {v10, v3}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v11
    invoke-virtual {v4, v11}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v11
    if-eqz v11, :find_lvl_next
    invoke-virtual {v10, v5}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v11
    invoke-virtual {v6, v11}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v11
    if-eqz v11, :find_lvl_next
    move-object v9, v10
    goto :lvl_checked

    :find_lvl_next
    add-int/lit8 v8, v8, 0x1
    goto :find_lvl

    :lvl_checked
    if-nez v9, :merge_tpls
    invoke-virtual {p0, v2}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;
    goto :next_add_lvl

    :merge_tpls
    const-string v4, "templates"
    invoke-virtual {v9, v4}, Lorg/json/JSONObject;->optJSONArray(Ljava/lang/String;)Lorg/json/JSONArray;
    move-result-object v6
    invoke-virtual {v2, v4}, Lorg/json/JSONObject;->optJSONArray(Ljava/lang/String;)Lorg/json/JSONArray;
    move-result-object v2
    if-eqz v6, :next_add_lvl
    if-eqz v2, :next_add_lvl

    invoke-virtual {v2}, Lorg/json/JSONArray;->length()I
    move-result v4
    const/4 v7, 0x0
    :loop_add_tpl
    if-ge v7, v4, :next_add_lvl
    invoke-virtual {v2, v7}, Lorg/json/JSONArray;->optJSONObject(I)Lorg/json/JSONObject;
    move-result-object v8
    if-eqz v8, :next_add_tpl
    invoke-virtual {v8, v3}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v9
    invoke-virtual {v8, v5}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v10

    invoke-virtual {v6}, Lorg/json/JSONArray;->length()I
    move-result v11
    const/4 v12, 0x0
    :find_tpl
    if-ge v12, v11, :tpl_not_found
    invoke-virtual {v6, v12}, Lorg/json/JSONArray;->optJSONObject(I)Lorg/json/JSONObject;
    move-result-object v13
    if-eqz v13, :find_tpl_next
    invoke-virtual {v13, v3}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v13
    invoke-virtual {v9, v13}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v13
    if-eqz v13, :find_tpl_next
    invoke-virtual {v6, v12}, Lorg/json/JSONArray;->optJSONObject(I)Lorg/json/JSONObject;
    move-result-object v13
    invoke-virtual {v13, v5}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v13
    invoke-virtual {v10, v13}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v13
    if-eqz v13, :find_tpl_next
    goto :next_add_tpl

    :find_tpl_next
    add-int/lit8 v12, v12, 0x1
    goto :find_tpl

    :tpl_not_found
    invoke-virtual {v6, v8}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    :next_add_tpl
    add-int/lit8 v7, v7, 0x1
    goto :loop_add_tpl

    :next_add_lvl
    add-int/lit8 v1, v1, 0x1
    goto :loop_add_lvl

    :done
    return-object p0
.end method

.method public static getDefaultJson(Landroid/content/Context;)Ljava/lang/String;
    .locals 3
    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    const-string v1, "rv_warn_server_cache"
    const-string v2, ""
    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0

    sget-object v1, Lru/fourpda/client/WarnTemplates;->lastServerLevels:[Lru/fourpda/client/x${'$'}i;
    if-eqz v1, :use_cache
    invoke-static {v1}, Lru/fourpda/client/WarnTemplates;->levelsToJsonArray([Lru/fourpda/client/x${'$'}i;)Lorg/json/JSONArray;
    move-result-object v1
    :try_start_0
    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result v2
    if-nez v2, :format_curr
    new-instance v2, Lorg/json/JSONArray;
    invoke-direct {v2, v0}, Lorg/json/JSONArray;-><init>(Ljava/lang/String;)V
    invoke-static {v1, v2}, Lru/fourpda/client/WarnTemplates;->mergeJsonArrays(Lorg/json/JSONArray;Lorg/json/JSONArray;)Lorg/json/JSONArray;
    move-result-object v1
    :format_curr
    const/4 v2, 0x2
    invoke-virtual {v1, v2}, Lorg/json/JSONArray;->toString(I)Ljava/lang/String;
    move-result-object v0
    return-object v0
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :use_cache

    :use_cache
    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result v1
    if-nez v1, :empty_arr
    :try_start_1
    new-instance v1, Lorg/json/JSONArray;
    invoke-direct {v1, v0}, Lorg/json/JSONArray;-><init>(Ljava/lang/String;)V
    const/4 v2, 0x2
    invoke-virtual {v1, v2}, Lorg/json/JSONArray;->toString(I)Ljava/lang/String;
    move-result-object v0
    return-object v0
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :ret_raw
    :ret_raw
    return-object v0

    :empty_arr
    const-string v0, "[]"
    return-object v0
.end method

.method public static updateToggleState(Lru/fourpda/client/x;)V
    .locals 3
    if-eqz p0, :done
    iget-object v0, p0, Lru/fourpda/client/v1;->h:Landroid/view/View;
    if-eqz v0, :done
    const v1, 0x7f060f01
    invoke-virtual {v0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v0
    instance-of v1, v0, Lru/fourpda/client/Widgets${'$'}CheckboxTextView;
    if-eqz v1, :done
    check-cast v0, Lru/fourpda/client/Widgets${'$'}CheckboxTextView;
    iget-object v1, p0, Lru/fourpda/client/x;->H:Lru/fourpda/client/MainActivity;
    invoke-static {v1}, Lru/fourpda/client/WarnTemplates;->isCustomEnabled(Landroid/content/Context;)Z
    move-result v1
    invoke-virtual {v0, v1}, Lru/fourpda/client/Widgets${'$'}CheckboxTextView;->setChecked(Z)V
    :done
    return-void
.end method

.method public static applyToDialog(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;)V
    .locals 4
    if-eqz p1, :done
    invoke-static {p1}, Lru/fourpda/client/WarnTemplates;->updateToggleState(Lru/fourpda/client/x;)V

    invoke-static {p0}, Lru/fourpda/client/WarnTemplates;->isCustomEnabled(Landroid/content/Context;)Z
    move-result v0
    const/4 v1, 0x0
    if-eqz v0, :restore_server

    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    const-string v2, "rv_warn_custom_json"
    const-string v3, ""
    invoke-interface {v0, v2, v3}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    invoke-static {v0}, Lru/fourpda/client/WarnTemplates;->parseLevels(Ljava/lang/String;)[Lru/fourpda/client/x${'$'}i;
    move-result-object v0
    if-eqz v0, :restore_server
    array-length v2, v0
    if-lez v2, :restore_server
    iput-object v0, p1, Lru/fourpda/client/x;->o:[Lru/fourpda/client/x${'$'}i;
    aget-object v0, v0, v1
    invoke-static {p1, v0}, Lru/fourpda/client/x;->i(Lru/fourpda/client/x;Lru/fourpda/client/x${'$'}i;)V
    return-void

    :restore_server
    sget-object v0, Lru/fourpda/client/WarnTemplates;->lastServerLevels:[Lru/fourpda/client/x${'$'}i;
    if-eqz v0, :done
    array-length v2, v0
    if-lez v2, :done
    iput-object v0, p1, Lru/fourpda/client/x;->o:[Lru/fourpda/client/x${'$'}i;
    aget-object v0, v0, v1
    invoke-static {p1, v0}, Lru/fourpda/client/x;->i(Lru/fourpda/client/x;Lru/fourpda/client/x${'$'}i;)V

    :done
    return-void
.end method

.method public static onWarnDialogInit(Lru/fourpda/client/x;)V
    .locals 10
    if-eqz p0, :done
    iget-object v0, p0, Lru/fourpda/client/x;->H:Lru/fourpda/client/MainActivity;
    if-eqz v0, :done

    iget-object v1, p0, Lru/fourpda/client/x;->o:[Lru/fourpda/client/x${'$'}i;
    sput-object v1, Lru/fourpda/client/WarnTemplates;->lastServerLevels:[Lru/fourpda/client/x${'$'}i;

    if-eqz v1, :setup_ui
    invoke-static {v1}, Lru/fourpda/client/WarnTemplates;->levelsToJsonArray([Lru/fourpda/client/x${'$'}i;)Lorg/json/JSONArray;
    move-result-object v1
    invoke-static {v0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v2
    const-string v3, "rv_warn_server_cache"
    const-string v4, ""
    invoke-interface {v2, v3, v4}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v4
    invoke-static {v4}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result v5
    if-nez v5, :save_cache
    :try_start
    new-instance v5, Lorg/json/JSONArray;
    invoke-direct {v5, v4}, Lorg/json/JSONArray;-><init>(Ljava/lang/String;)V
    invoke-static {v5, v1}, Lru/fourpda/client/WarnTemplates;->mergeJsonArrays(Lorg/json/JSONArray;Lorg/json/JSONArray;)Lorg/json/JSONArray;
    move-result-object v1
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :save_cache
    :save_cache
    invoke-interface {v2}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
    move-result-object v2
    invoke-virtual {v1}, Lorg/json/JSONArray;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-interface {v2, v3, v1}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
    invoke-interface {v2}, Landroid/content/SharedPreferences${'$'}Editor;->commit()Z

    :setup_ui
    iget-object v1, p0, Lru/fourpda/client/v1;->h:Landroid/view/View;
    instance-of v2, v1, Landroid/widget/LinearLayout;
    if-eqz v2, :apply_levels
    move-object v2, v1
    check-cast v2, Landroid/widget/LinearLayout;
    const v3, 0x7f060f01
    invoke-virtual {v2, v3}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v4
    if-nez v4, :apply_levels

    invoke-virtual {v0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;
    move-result-object v4
    invoke-virtual {v4}, Landroid/content/res/Resources;->getDisplayMetrics()Landroid/util/DisplayMetrics;
    move-result-object v4
    iget v4, v4, Landroid/util/DisplayMetrics;->density:F

    invoke-virtual {v2}, Landroid/view/ViewGroup;->getChildCount()I
    move-result v5
    const/4 v6, 0x0
    if-lez v5, :create_row
    invoke-virtual {v2, v6}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;
    move-result-object v5
    if-eqz v5, :create_row
    const/high16 v7, 0x40c00000    # 6.0f
    mul-float v7, v7, v4
    float-to-int v7, v7
    invoke-virtual {v5, v6, v6, v6, v7}, Landroid/view/View;->setPadding(IIII)V

    :create_row
    new-instance v5, Landroid/widget/LinearLayout;
    invoke-direct {v5, v0}, Landroid/widget/LinearLayout;-><init>(Landroid/content/Context;)V
    invoke-virtual {v5, v6}, Landroid/widget/LinearLayout;->setOrientation(I)V
    const/16 v7, 0x10
    invoke-virtual {v5, v7}, Landroid/widget/LinearLayout;->setGravity(I)V
    const/high16 v7, 0x41200000    # 10.0f
    mul-float v7, v7, v4
    float-to-int v7, v7
    invoke-virtual {v5, v6, v6, v6, v7}, Landroid/view/View;->setPadding(IIII)V

    new-instance v7, Lru/fourpda/client/Widgets${'$'}CheckboxTextView;
    invoke-direct {v7, v0}, Lru/fourpda/client/Widgets${'$'}CheckboxTextView;-><init>(Landroid/content/Context;)V
    invoke-virtual {v7, v3}, Landroid/view/View;->setId(I)V
    const-string v3, "Свои шаблоны"
    invoke-virtual {v7, v3}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    const/high16 v3, 0x41800000    # 16.0f
    invoke-virtual {v7, v3}, Landroid/widget/TextView;->setTextSize(F)V
    const/16 v3, 0x13
    invoke-virtual {v7, v3}, Landroid/widget/TextView;->setGravity(I)V
    invoke-virtual {v7, v6}, Lru/fourpda/client/Widgets${'$'}CheckboxTextView;->setBoxAlign(I)V

    const/high16 v3, 0x42280000    # 42.0f
    mul-float v3, v3, v4
    float-to-int v3, v3

    sget v8, Lru/fourpda/client/e1;->I0:I
    invoke-virtual {v2, v8}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v8
    instance-of v9, v8, Landroid/widget/TextView;
    if-eqz v9, :set_cb_lp
    check-cast v8, Landroid/widget/TextView;
    invoke-virtual {v8}, Landroid/widget/TextView;->getTextColors()Landroid/content/res/ColorStateList;
    move-result-object v9
    if-eqz v9, :copy_cb_bg
    invoke-virtual {v7, v9}, Landroid/widget/TextView;->setTextColor(Landroid/content/res/ColorStateList;)V
    :copy_cb_bg
    invoke-virtual {v8}, Landroid/view/View;->getBackground()Landroid/graphics/drawable/Drawable;
    move-result-object v8
    if-eqz v8, :set_cb_lp
    invoke-virtual {v8}, Landroid/graphics/drawable/Drawable;->getConstantState()Landroid/graphics/drawable/Drawable${'$'}ConstantState;
    move-result-object v8
    if-eqz v8, :set_cb_lp
    invoke-virtual {v8}, Landroid/graphics/drawable/Drawable${'$'}ConstantState;->newDrawable()Landroid/graphics/drawable/Drawable;
    move-result-object v8
    invoke-virtual {v7, v8}, Landroid/view/View;->setBackgroundDrawable(Landroid/graphics/drawable/Drawable;)V

    :set_cb_lp
    new-instance v8, Landroid/widget/LinearLayout${'$'}LayoutParams;
    const/high16 v9, 0x3f800000    # 1.0f
    invoke-direct {v8, v6, v3, v9}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(IIF)V
    invoke-virtual {v7, v8}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V

    new-instance v8, Lru/fourpda/client/WarnTemplates;
    const/4 v9, 0x3
    invoke-direct {v8, v0, p0, v9}, Lru/fourpda/client/WarnTemplates;-><init>(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;I)V
    invoke-virtual {v7, v8}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
    invoke-virtual {v5, v7}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

    new-instance v7, Landroid/widget/TextView;
    invoke-direct {v7, v0}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V
    const-string v8, "JSON ⚙"
    invoke-virtual {v7, v8}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    const/high16 v8, 0x41700000    # 15.0f
    invoke-virtual {v7, v8}, Landroid/widget/TextView;->setTextSize(F)V
    const/4 v8, 0x0
    const/4 v9, 0x1
    invoke-virtual {v7, v8, v9}, Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;I)V
    sget-object v8, Lru/fourpda/client/k1${'$'}a;->k0:Landroid/content/res/ColorStateList;
    invoke-virtual {v7, v8}, Landroid/widget/TextView;->setTextColor(Landroid/content/res/ColorStateList;)V
    const/16 v8, 0x11
    invoke-virtual {v7, v8}, Landroid/widget/TextView;->setGravity(I)V
    const/high16 v8, 0x41400000    # 12.0f
    mul-float v4, v4, v8
    float-to-int v4, v4
    invoke-virtual {v7, v4, v6, v4, v6}, Landroid/view/View;->setPadding(IIII)V
    invoke-virtual {v7, v9}, Landroid/view/View;->setClickable(Z)V
    new-instance v4, Landroid/widget/LinearLayout${'$'}LayoutParams;
    const/4 v6, -0x2
    invoke-direct {v4, v6, v3}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(II)V
    invoke-virtual {v7, v4}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V

    new-instance v3, Lru/fourpda/client/WarnTemplates;
    invoke-direct {v3, v0, p0, v9}, Lru/fourpda/client/WarnTemplates;-><init>(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;I)V
    invoke-virtual {v7, v3}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V
    invoke-virtual {v5, v7}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

    new-instance v4, Landroid/widget/LinearLayout${'$'}LayoutParams;
    const/4 v7, -0x1
    invoke-direct {v4, v7, v6}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(II)V
    invoke-virtual {v2, v5, v9, v4}, Landroid/view/ViewGroup;->addView(Landroid/view/View;ILandroid/view/ViewGroup${'$'}LayoutParams;)V

    sget v4, Lru/fourpda/client/e1;->V0:I
    invoke-virtual {v1, v4}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v1
    if-eqz v1, :apply_levels
    invoke-virtual {v1, v9}, Landroid/view/View;->setLongClickable(Z)V
    invoke-virtual {v1, v3}, Landroid/view/View;->setOnLongClickListener(Landroid/view/View${'$'}OnLongClickListener;)V

    :apply_levels
    invoke-static {v0, p0}, Lru/fourpda/client/WarnTemplates;->applyToDialog(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;)V

    :done
    return-void
.end method

.method public static showMenu(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;)V
    .locals 9
    new-instance v0, Lru/fourpda/client/WarnTemplates;
    const/4 v1, 0x1
    invoke-direct {v0, p0, p1, v1}, Lru/fourpda/client/WarnTemplates;-><init>(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;I)V
    new-instance v2, Lru/fourpda/client/B1;
    invoke-direct {v2, p0, v0}, Lru/fourpda/client/B1;-><init>(Lru/fourpda/client/MainActivity;Lru/fourpda/client/B1${'$'}b;)V

    invoke-static {p0}, Lru/fourpda/client/WarnTemplates;->isCustomEnabled(Landroid/content/Context;)Z
    move-result v0

    if-eqz v0, :hdr_srv
    const-string v6, "Режим: СВОИ ШАБЛОНЫ (JSON)"
    goto :add_hdr
    :hdr_srv
    const-string v6, "Режим: СЕРВЕРНЫЕ ШАБЛОНЫ"
    :add_hdr
    const/4 v3, 0x0
    const/4 v4, 0x0
    const/4 v5, 0x0
    const/4 v7, 0x1
    const/4 v8, 0x0
    invoke-virtual/range {v2 .. v8}, Lru/fourpda/client/B1;->c(IIILjava/lang/String;ZZ)V

    const/4 v3, 0x2
    const/4 v4, 0x0
    const-string v5, "Редактировать JSON шаблонов"
    invoke-virtual {v2, v3, v1, v4, v5}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V

    const-string v5, "Скопировать JSON в буфер"
    invoke-virtual {v2, v3, v3, v4, v5}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V

    const/4 v1, 0x3
    const-string v5, "Вставить JSON из буфера"
    invoke-virtual {v2, v3, v1, v4, v5}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V

    const/4 v1, 0x4
    const-string v5, "Дополнить шаблонами с сервера"
    invoke-virtual {v2, v3, v1, v4, v5}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V

    const/4 v1, 0x5
    const-string v5, "Скопировать оригинал с сервера"
    invoke-virtual {v2, v3, v1, v4, v5}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V

    const/4 v1, 0x6
    const-string v5, "Очистить свои шаблоны (сброс)"
    invoke-virtual {v2, v3, v1, v4, v5}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V

    const/4 v0, 0x0
    invoke-virtual {v2, v0}, Lru/fourpda/client/B1;->e(Landroid/view/View;)Z
    return-void
.end method

.method public static showEditorDialog(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;)V
    .locals 7
    new-instance v0, Lru/fourpda/client/C1;
    const-string v2, "JSON шаблонов наказаний"
    const/4 v3, 0x0
    const-string v4, "СОХРАНИТЬ"
    const-string v5, "ОТМЕНА"
    move-object v1, p0
    invoke-direct/range {v0 .. v5}, Lru/fourpda/client/C1;-><init>(Lru/fourpda/client/MainActivity;Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;)V

    iget-object v1, v0, Lru/fourpda/client/C1;->k:Landroid/widget/TextView;
    if-eqz v1, :setup_edit
    const-string v2, "Поля шаблона: id, name, reason, message, flags, premod (ч), ro (ч).\nПустое поле и Сохранить = очистка своих шаблонов."
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    :setup_edit
    iget-object v1, v0, Lru/fourpda/client/C1;->l:Landroid/widget/EditText;
    const/4 v2, 0x0
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setSingleLine(Z)V
    const v3, 0xa0001
    invoke-virtual {v1, v3}, Landroid/widget/TextView;->setInputType(I)V
    const/16 v3, 0x8
    invoke-virtual {v1, v3}, Landroid/widget/TextView;->setMinLines(I)V
    const/16 v3, 0xe
    invoke-virtual {v1, v3}, Landroid/widget/TextView;->setMaxLines(I)V
    const/16 v3, 0x33
    invoke-virtual {v1, v3}, Landroid/widget/TextView;->setGravity(I)V
    const/4 v3, 0x2
    const/high16 v4, 0x41500000    # 13.0f
    invoke-virtual {v1, v3, v4}, Landroid/widget/TextView;->setTextSize(IF)V

    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v4
    const-string v5, "rv_warn_custom_json"
    const-string v6, ""
    invoke-interface {v4, v5, v6}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v4
    invoke-static {v4}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result v5
    if-eqz v5, :set_json_text
    invoke-static {p0}, Lru/fourpda/client/WarnTemplates;->getDefaultJson(Landroid/content/Context;)Ljava/lang/String;
    move-result-object v4
    :set_json_text
    invoke-virtual {v1, v4}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    new-instance v1, Lru/fourpda/client/WarnTemplates;
    invoke-direct {v1, p0, p1, v3}, Lru/fourpda/client/WarnTemplates;-><init>(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;I)V
    iput-object v0, v1, Lru/fourpda/client/WarnTemplates;->editDlg:Lru/fourpda/client/C1;
    invoke-virtual {v0, v1, v2}, Lru/fourpda/client/v1;->g(Landroid/view/View${'$'}OnClickListener;Z)V
    const/4 v1, 0x1
    invoke-virtual {v0, v1, v1, v1}, Lru/fourpda/client/v1;->c(ZZZ)V
    return-void
.end method

.method public static saveCustomJson(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;Ljava/lang/String;)Z
    .locals 4
    if-nez p2, :trim_str
    const-string p2, ""
    :trim_str
    invoke-virtual {p2}, Ljava/lang/String;->trim()Ljava/lang/String;
    move-result-object p2
    invoke-static {p2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result v0
    const/4 v1, 0x0
    const/4 v2, 0x1
    if-eqz v0, :validate_json

    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object p2
    invoke-interface {p2}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
    move-result-object p2
    const-string v0, "rv_warn_custom_json"
    invoke-interface {p2, v0}, Landroid/content/SharedPreferences${'$'}Editor;->remove(Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
    const-string v0, "rv_warn_use_custom"
    invoke-interface {p2, v0, v1}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
    invoke-interface {p2}, Landroid/content/SharedPreferences${'$'}Editor;->commit()Z
    invoke-static {p0, p1}, Lru/fourpda/client/WarnTemplates;->applyToDialog(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;)V
    const-string p1, "Свои шаблоны очищены, включены серверные"
    invoke-static {p0, p1, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object p0
    invoke-virtual {p0}, Landroid/widget/Toast;->show()V
    return v2

    :validate_json
    invoke-static {p2}, Lru/fourpda/client/WarnTemplates;->parseLevels(Ljava/lang/String;)[Lru/fourpda/client/x${'$'}i;
    move-result-object p2
    if-eqz p2, :invalid_json
    array-length v0, p2
    if-lez v0, :invalid_json

    invoke-static {p2}, Lru/fourpda/client/WarnTemplates;->levelsToJsonArray([Lru/fourpda/client/x${'$'}i;)Lorg/json/JSONArray;
    move-result-object p2
    :try_start
    const/4 v0, 0x2
    invoke-virtual {p2, v0}, Lorg/json/JSONArray;->toString(I)Ljava/lang/String;
    move-result-object p2
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :fallback_str
    goto :store_json
    :fallback_str
    invoke-virtual {p2}, Lorg/json/JSONArray;->toString()Ljava/lang/String;
    move-result-object p2

    :store_json
    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
    move-result-object v0
    const-string v3, "rv_warn_custom_json"
    invoke-interface {v0, v3, p2}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
    const-string v3, "rv_warn_use_custom"
    invoke-interface {v0, v3, v2}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
    invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->commit()Z
    invoke-static {p0, p1}, Lru/fourpda/client/WarnTemplates;->applyToDialog(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;)V
    const-string p1, "Свои шаблоны сохранены и включены"
    invoke-static {p0, p1, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object p0
    invoke-virtual {p0}, Landroid/widget/Toast;->show()V
    return v2

    :invalid_json
    const-string p1, "Ошибка в JSON! Проверьте синтаксис и наличие templates"
    invoke-static {p0, p1, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object p0
    invoke-virtual {p0}, Landroid/widget/Toast;->show()V
    return v1
.end method

.method public static copyToClipboard(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V
    .locals 2
    const-string v0, "clipboard"
    invoke-virtual {p0, v0}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
    move-result-object v0
    check-cast v0, Landroid/content/ClipboardManager;
    const-string v1, "4PDA Warn Templates"
    invoke-static {v1, p1}, Landroid/content/ClipData;->newPlainText(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Landroid/content/ClipData;
    move-result-object v1
    invoke-virtual {v0, v1}, Landroid/content/ClipboardManager;->setPrimaryClip(Landroid/content/ClipData;)V
    const/4 v0, 0x0
    invoke-static {p0, p2, v0}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object p0
    invoke-virtual {p0}, Landroid/widget/Toast;->show()V
    return-void
.end method

.method public a(III)V
    .locals 6
    iget-object v0, p0, Lru/fourpda/client/WarnTemplates;->act:Lru/fourpda/client/MainActivity;
    if-eqz v0, :done
    iget-object v1, p0, Lru/fourpda/client/WarnTemplates;->warnDlg:Lru/fourpda/client/x;

    const/4 v2, 0x1
    if-ne p2, v2, :check_copy
    invoke-static {v0, v1}, Lru/fourpda/client/WarnTemplates;->showEditorDialog(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;)V
    return-void

    :check_copy
    const/4 v3, 0x2
    if-ne p2, v3, :check_paste
    invoke-static {v0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object p1
    const-string p2, "rv_warn_custom_json"
    const-string p3, ""
    invoke-interface {p1, p2, p3}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object p1
    invoke-static {p1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result p2
    if-eqz p2, :do_copy_curr
    invoke-static {v0}, Lru/fourpda/client/WarnTemplates;->getDefaultJson(Landroid/content/Context;)Ljava/lang/String;
    move-result-object p1
    :do_copy_curr
    const-string p2, "JSON шаблонов скопирован в буфер"
    invoke-static {v0, p1, p2}, Lru/fourpda/client/WarnTemplates;->copyToClipboard(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V
    return-void

    :check_paste
    const/4 v4, 0x3
    const/4 v5, 0x0
    if-ne p2, v4, :check_merge
    const-string p1, "clipboard"
    invoke-virtual {v0, p1}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
    move-result-object p1
    check-cast p1, Landroid/content/ClipboardManager;
    invoke-virtual {p1}, Landroid/content/ClipboardManager;->getPrimaryClip()Landroid/content/ClipData;
    move-result-object p1
    if-eqz p1, :empty_clip
    invoke-virtual {p1}, Landroid/content/ClipData;->getItemCount()I
    move-result p2
    if-lez p2, :empty_clip
    invoke-virtual {p1, v5}, Landroid/content/ClipData;->getItemAt(I)Landroid/content/ClipData${'$'}Item;
    move-result-object p1
    invoke-virtual {p1, v0}, Landroid/content/ClipData${'$'}Item;->coerceToText(Landroid/content/Context;)Ljava/lang/CharSequence;
    move-result-object p1
    if-eqz p1, :empty_clip
    invoke-interface {p1}, Ljava/lang/CharSequence;->toString()Ljava/lang/String;
    move-result-object p1
    invoke-static {v0, v1, p1}, Lru/fourpda/client/WarnTemplates;->saveCustomJson(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;Ljava/lang/String;)Z
    return-void
    :empty_clip
    const-string p1, "Буфер обмена пуст"
    invoke-static {v0, p1, v5}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object p1
    invoke-virtual {p1}, Landroid/widget/Toast;->show()V
    return-void

    :check_merge
    const/4 v4, 0x4
    if-ne p2, v4, :check_copy_srv
    invoke-static {v0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object p1
    const-string p2, "rv_warn_custom_json"
    const-string p3, ""
    invoke-interface {p1, p2, p3}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object p1
    invoke-static {v0}, Lru/fourpda/client/WarnTemplates;->getDefaultJson(Landroid/content/Context;)Ljava/lang/String;
    move-result-object p2
    :try_start_m
    invoke-static {p1}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result p3
    if-nez p3, :merge_use_def
    new-instance p3, Lorg/json/JSONArray;
    invoke-direct {p3, p1}, Lorg/json/JSONArray;-><init>(Ljava/lang/String;)V
    new-instance p1, Lorg/json/JSONArray;
    invoke-direct {p1, p2}, Lorg/json/JSONArray;-><init>(Ljava/lang/String;)V
    invoke-static {p3, p1}, Lru/fourpda/client/WarnTemplates;->mergeJsonArrays(Lorg/json/JSONArray;Lorg/json/JSONArray;)Lorg/json/JSONArray;
    move-result-object p1
    invoke-virtual {p1, v3}, Lorg/json/JSONArray;->toString(I)Ljava/lang/String;
    move-result-object p2
    :merge_use_def
    invoke-static {v0, v1, p2}, Lru/fourpda/client/WarnTemplates;->saveCustomJson(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;Ljava/lang/String;)Z
    :try_end_m
    .catch Ljava/lang/Exception; {:try_start_m .. :try_end_m} :done
    return-void

    :check_copy_srv
    const/4 v2, 0x5
    if-ne p2, v2, :check_reset
    invoke-static {v0}, Lru/fourpda/client/WarnTemplates;->getDefaultJson(Landroid/content/Context;)Ljava/lang/String;
    move-result-object p1
    const-string p2, "Оригинальные шаблоны сервера скопированы"
    invoke-static {v0, p1, p2}, Lru/fourpda/client/WarnTemplates;->copyToClipboard(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V
    return-void

    :check_reset
    const/4 v2, 0x6
    if-ne p2, v2, :done
    const-string p1, ""
    invoke-static {v0, v1, p1}, Lru/fourpda/client/WarnTemplates;->saveCustomJson(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;Ljava/lang/String;)Z

    :done
    return-void
.end method

.method public onClick(Landroid/view/View;)V
    .locals 5
    iget-object v0, p0, Lru/fourpda/client/WarnTemplates;->act:Lru/fourpda/client/MainActivity;
    if-eqz v0, :done
    iget-object v1, p0, Lru/fourpda/client/WarnTemplates;->warnDlg:Lru/fourpda/client/x;
    iget v2, p0, Lru/fourpda/client/WarnTemplates;->mode:I

    const/4 v3, 0x3
    if-ne v2, v3, :check_save_mode
    instance-of v2, p1, Lru/fourpda/client/Widgets${'$'}CheckboxTextView;
    if-eqz v2, :done
    check-cast p1, Lru/fourpda/client/Widgets${'$'}CheckboxTextView;
    invoke-virtual {p1}, Lru/fourpda/client/Widgets${'$'}CheckboxTextView;->getChecked()Z
    move-result v2
    const/4 v3, 0x0
    if-eqz v2, :disable_custom

    invoke-static {v0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v2
    const-string v4, "rv_warn_custom_json"
    const-string p1, ""
    invoke-interface {v2, v4, p1}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v2
    invoke-static {v2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result v2
    if-eqz v2, :enable_existing
    invoke-static {v0, v3}, Lru/fourpda/client/WarnTemplates;->setCustomEnabled(Landroid/content/Context;Z)V
    invoke-static {v1}, Lru/fourpda/client/WarnTemplates;->updateToggleState(Lru/fourpda/client/x;)V
    invoke-static {v0, v1}, Lru/fourpda/client/WarnTemplates;->showEditorDialog(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;)V
    return-void

    :enable_existing
    const/4 v2, 0x1
    invoke-static {v0, v2}, Lru/fourpda/client/WarnTemplates;->setCustomEnabled(Landroid/content/Context;Z)V
    invoke-static {v0, v1}, Lru/fourpda/client/WarnTemplates;->applyToDialog(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;)V
    return-void

    :disable_custom
    invoke-static {v0, v3}, Lru/fourpda/client/WarnTemplates;->setCustomEnabled(Landroid/content/Context;Z)V
    invoke-static {v0, v1}, Lru/fourpda/client/WarnTemplates;->applyToDialog(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;)V
    return-void

    :check_save_mode
    const/4 v3, 0x2
    if-ne v2, v3, :open_menu
    iget-object v2, p0, Lru/fourpda/client/WarnTemplates;->editDlg:Lru/fourpda/client/C1;
    if-eqz v2, :done
    iget-object v3, v2, Lru/fourpda/client/C1;->l:Landroid/widget/EditText;
    invoke-virtual {v3}, Landroid/widget/EditText;->getText()Landroid/text/Editable;
    move-result-object v3
    invoke-virtual {v3}, Ljava/lang/Object;->toString()Ljava/lang/String;
    move-result-object v3
    invoke-static {v0, v1, v3}, Lru/fourpda/client/WarnTemplates;->saveCustomJson(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;Ljava/lang/String;)Z
    move-result v0
    if-eqz v0, :done
    invoke-virtual {v2}, Lru/fourpda/client/v1;->a()V
    return-void

    :open_menu
    invoke-static {v0, v1}, Lru/fourpda/client/WarnTemplates;->showMenu(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;)V
    :done
    return-void
.end method

.method public onLongClick(Landroid/view/View;)Z
    .locals 2
    iget-object v0, p0, Lru/fourpda/client/WarnTemplates;->act:Lru/fourpda/client/MainActivity;
    if-eqz v0, :not_handled
    iget-object v1, p0, Lru/fourpda/client/WarnTemplates;->warnDlg:Lru/fourpda/client/x;
    invoke-static {v0, v1}, Lru/fourpda/client/WarnTemplates;->showMenu(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;)V
    const/4 v0, 0x1
    return v0
    :not_handled
    const/4 v0, 0x0
    return v0
.end method
"""

val customWarnTemplatesPatch = bytecodePatch(
    name = "Свои шаблоны наказаний (JSON)",
    description = "Добавляет в окно «Наказать» переключатель «Свои шаблоны» и кнопку «JSON ⚙» для редактирования, копирования и подмены списка уровней и шаблонов через нативное модальное окно JSON.",
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        if (classDefs[WARN_TEMPLATES] == null) {
            classDefs += assembleWarnTemplates(WARN_TEMPLATES_SMALI)
        }

        val warnDialogDef = classDefs[WARN_DIALOG]
            ?: error("не найден класс окна наказания x")
        val warnMutable = proxy(warnDialogDef).mutableClass

        val levelsField = warnMutable.fields.firstOrNull { field ->
            field.name == "o"
        } ?: error("не найдено поле уровней x.o")
        levelsField.accessFlags = levelsField.accessFlags and AccessFlags.PRIVATE.value.inv()

        val warnInit = warnMutable.methods.firstOrNull { method ->
            method.name == "<init>" && method.parameterTypes.size == 7
        } ?: error("не найден конструктор окна наказания x.<init>")

        val returnIdx = warnInit.implementation!!.instructions.indexOfLast { instruction ->
            instruction.opcode == Opcode.RETURN_VOID
        }
        if (returnIdx < 0) {
            error("не найден return-void в конструкторе x.<init>")
        }
        warnInit.addInstructions(
            returnIdx,
            "invoke-static {p0}, $WARN_TEMPLATES->onWarnDialogInit(Lru/fourpda/client/x;)V",
        )

        val settingsDef = classDefs[SETTINGS_PAGE]
            ?: error("не найден класс настроек I0")
        val settingsMenuMethod = proxy(settingsDef).mutableClass.methods.firstOrNull { method ->
            method.name == "K" && method.parameterTypes.size == 1
        } ?: error("не найден метод меню настроек I0.K")

        val showMenuIdx = settingsMenuMethod.implementation!!.instructions.indexOfFirst { instruction ->
            val called = instruction.calledMethod()
            called?.definingClass == "Lru/fourpda/client/B1;" && called.name == "e"
        }
        if (showMenuIdx >= 0) {
            settingsMenuMethod.addInstructions(
                showMenuIdx,
                """
                const/16 v1, 0x4d
                const-string v2, "Шаблоны наказаний (JSON)"
                invoke-virtual {v0, v6, v6, v1, v2}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V
                """.trimIndent(),
            )
        }

        val settingsCbDef = classDefs[SETTINGS_MENU_CB]
            ?: error("не найден обработчик меню настроек I0\$D")
        val settingsCbMethod = proxy(settingsCbDef).mutableClass.methods.firstOrNull { method ->
            method.name == "a" && method.parameterTypes.size == 3
        } ?: error("не найден метод I0\$D.a")

        settingsCbMethod.addInstructions(
            0,
            """
            const/16 v0, 0x4d
            if-ne p3, v0, :not_warn_templates
            iget-object v0, p0, Lru/fourpda/client/I0${'$'}D;->a:Lru/fourpda/client/I0;
            iget-object v0, v0, Lru/fourpda/client/w0;->g:Lru/fourpda/client/MainActivity;
            const/4 v1, 0x0
            invoke-static {v0, v1}, $WARN_TEMPLATES->showMenu(Lru/fourpda/client/MainActivity;Lru/fourpda/client/x;)V
            return-void
            :not_warn_templates
            """.trimIndent(),
        )
    }
}

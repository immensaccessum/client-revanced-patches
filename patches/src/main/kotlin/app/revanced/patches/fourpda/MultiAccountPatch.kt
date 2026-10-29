package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
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

private const val MULTI_ACCOUNT = "Lru/fourpda/client/MultiAccount;"
private const val PROFILE_CALLBACK = "Lru/fourpda/client/MainActivity\$u;"
private const val SESSION_CORE = "Lru/fourpda/client/F;"
private const val AUTH_PACKET = "Lru/fourpda/client/F\$b;"

private fun Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun assemble(smali: String): ClassDef {
    val reader = StringReader(smali)
    val lexer = smaliFlexLexer(reader, 15)
    val tokens = CommonTokenStream(lexer as TokenSource)
    val parser = smaliParser(tokens)
    val fileTree = parser.smali_file()
    if (lexer.numberOfSyntaxErrors > 0 || parser.numberOfSyntaxErrors > 0) {
        error("ошибка сборки MultiAccount")
    }
    val walker = smaliTreeWalker(
        CommonTreeNodeStream(fileTree.tree).apply { tokenStream = tokens },
    )
    walker.setDexBuilder(DexBuilder(Opcodes.getDefault()))
    return walker.smali_file()
}

private const val MULTI_ACCOUNT_SMALI = """
.class public Lru/fourpda/client/MultiAccount;
.super Ljava/lang/Object;
.implements Landroid/view/View${'$'}OnClickListener;
.implements Landroid/view/View${'$'}OnLongClickListener;
.implements Lru/fourpda/client/B1${'$'}b;
.implements Ljava/lang/Runnable;

.field public act:Lru/fourpda/client/MainActivity;
.field public mode:I
.field public dlg:Lru/fourpda/client/C1;
.field public edit2:Landroid/widget/EditText;

.method public constructor <init>(Lru/fourpda/client/MainActivity;I)V
    .registers 3
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    iput-object p1, p0, Lru/fourpda/client/MultiAccount;->act:Lru/fourpda/client/MainActivity;
    iput p2, p0, Lru/fourpda/client/MultiAccount;->mode:I
    return-void
.end method

.method public static safeDisconnect()V
    .registers 2
    sget-object v0, Lru/fourpda/client/F;->x:Lru/fourpda/client/F;
    if-eqz v0, :done
    invoke-virtual {v0}, Lru/fourpda/client/n0;->y()Z
    move-result v1
    if-nez v1, :done
    invoke-virtual {v0}, Lru/fourpda/client/n0;->t()V
    :done
    return-void
.end method

.method public static loadArray(Landroid/content/Context;)Lorg/json/JSONArray;
    .registers 4
    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    const-string v1, "rv_multi_accounts"
    const-string v2, "[]"
    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    :try_start
    new-instance v1, Lorg/json/JSONArray;
    invoke-direct {v1, v0}, Lorg/json/JSONArray;-><init>(Ljava/lang/String;)V
    return-object v1
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :catch_err
    :catch_err
    new-instance v1, Lorg/json/JSONArray;
    invoke-direct {v1}, Lorg/json/JSONArray;-><init>()V
    return-object v1
.end method

.method public static saveAccount(Landroid/content/Context;ILjava/lang/String;Ljava/lang/String;Z)V
    .registers 14
    if-lez p1, :done
    invoke-static {p2}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result v0
    if-nez v0, :done

    invoke-static {p3}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result v0
    if-eqz v0, :has_name
    new-instance v0, Ljava/lang/StringBuilder;
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
    const-string v1, "ID "
    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object p3
    :has_name

    invoke-static {p0}, Lru/fourpda/client/MultiAccount;->loadArray(Landroid/content/Context;)Lorg/json/JSONArray;
    move-result-object v0
    new-instance v1, Lorg/json/JSONArray;
    invoke-direct {v1}, Lorg/json/JSONArray;-><init>()V
    invoke-virtual {v0}, Lorg/json/JSONArray;->length()I
    move-result v2
    const/4 v3, 0x0

    :loop
    if-ge v3, v2, :after_loop
    invoke-virtual {v0, v3}, Lorg/json/JSONArray;->optJSONObject(I)Lorg/json/JSONObject;
    move-result-object v4
    if-eqz v4, :next
    const-string v5, "id"
    invoke-virtual {v4, v5}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;)I
    move-result v5
    if-ne v5, p1, :keep_other
    const-string v5, "ID "
    invoke-virtual {p3, v5}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
    move-result v6
    if-eqz v6, :next
    const-string v6, "name"
    invoke-virtual {v4, v6}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v6
    invoke-static {v6}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result v7
    if-nez v7, :next
    invoke-virtual {v6, v5}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
    move-result v5
    if-nez v5, :next
    move-object p3, v6
    goto :next

    :keep_other
    invoke-virtual {v1, v4}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    :next
    add-int/lit8 v3, v3, 0x1
    goto :loop

    :after_loop
    :try_start
    new-instance v4, Lorg/json/JSONObject;
    invoke-direct {v4}, Lorg/json/JSONObject;-><init>()V
    const-string v5, "id"
    invoke-virtual {v4, v5, p1}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;
    const-string v5, "key"
    invoke-virtual {v4, v5, p2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;
    const-string v5, "name"
    invoke-virtual {v4, v5, p3}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;
    const-string v5, "hidden"
    invoke-virtual {v4, v5, p4}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;
    invoke-virtual {v1, v4}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;

    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v5
    invoke-interface {v5}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
    move-result-object v5
    const-string v6, "rv_multi_accounts"
    invoke-virtual {v1}, Lorg/json/JSONArray;->toString()Ljava/lang/String;
    move-result-object v7
    invoke-interface {v5, v6, v7}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
    invoke-interface {v5}, Landroid/content/SharedPreferences${'$'}Editor;->commit()Z
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :done

    :done
    return-void
.end method

.method public static syncCurrent(Landroid/content/Context;)V
    .registers 7
    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    const-string v1, "member_id"
    const/4 v2, 0x0
    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I
    move-result v1
    if-lez v1, :done
    const-string v3, "login_key"
    const-string v4, ""
    invoke-interface {v0, v3, v4}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v3
    const-string v5, "member_hidden"
    invoke-interface {v0, v5, v2}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
    move-result v2
    const-string v5, "member_info_name"
    invoke-interface {v0, v5, v4}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v4

    sget-object v5, Lru/fourpda/client/F;->x:Lru/fourpda/client/F;
    if-eqz v5, :save
    iget-object v5, v5, Lru/fourpda/client/F;->r:Lru/fourpda/client/F${'$'}h;
    if-eqz v5, :save
    iget v0, v5, Lru/fourpda/client/F${'$'}h;->a:I
    if-ne v0, v1, :save
    iget-object v0, v5, Lru/fourpda/client/F${'$'}h;->b:Ljava/lang/String;
    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result v5
    if-nez v5, :save
    move-object v4, v0

    :save
    invoke-static {p0, v1, v3, v4, v2}, Lru/fourpda/client/MultiAccount;->saveAccount(Landroid/content/Context;ILjava/lang/String;Ljava/lang/String;Z)V
    :done
    return-void
.end method

.method public static onProfile(Lru/fourpda/client/MainActivity;Lru/fourpda/client/F${'$'}h;)V
    .registers 10
    if-eqz p0, :done
    if-eqz p1, :sync_fallback
    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    const-string v1, "login_key"
    const-string v2, ""
    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v1
    const-string v2, "member_hidden"
    const/4 v3, 0x0
    invoke-interface {v0, v2, v3}, Landroid/content/SharedPreferences;->getBoolean(Ljava/lang/String;Z)Z
    move-result v2
    iget v3, p1, Lru/fourpda/client/F${'$'}h;->a:I
    iget-object v4, p1, Lru/fourpda/client/F${'$'}h;->b:Ljava/lang/String;
    invoke-static {p0, v3, v1, v4, v2}, Lru/fourpda/client/MultiAccount;->saveAccount(Landroid/content/Context;ILjava/lang/String;Ljava/lang/String;Z)V
    goto :setup_ui

    :sync_fallback
    invoke-static {p0}, Lru/fourpda/client/MultiAccount;->syncCurrent(Landroid/content/Context;)V

    :setup_ui
    sget v0, Lru/fourpda/client/e1;->W2:I
    invoke-virtual {p0, v0}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v0
    if-eqz v0, :check_menu
    const/4 v1, 0x1
    invoke-virtual {v0, v1}, Landroid/view/View;->setLongClickable(Z)V
    new-instance v1, Lru/fourpda/client/MultiAccount;
    const/4 v2, 0x0
    invoke-direct {v1, p0, v2}, Lru/fourpda/client/MultiAccount;-><init>(Lru/fourpda/client/MainActivity;I)V
    invoke-virtual {v0, v1}, Landroid/view/View;->setOnLongClickListener(Landroid/view/View${'$'}OnLongClickListener;)V

    :check_menu
    sget v0, Lru/fourpda/client/e1;->l3:I
    invoke-virtual {p0, v0}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v0
    instance-of v1, v0, Landroid/widget/LinearLayout;
    if-eqz v1, :done
    check-cast v0, Landroid/widget/LinearLayout;

    sget v1, Lru/fourpda/client/e1;->k3:I
    invoke-virtual {v0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v1
    instance-of v2, v1, Landroid/widget/TextView;
    if-eqz v2, :done
    check-cast v1, Landroid/widget/TextView;

    const v2, 0x7f060fff
    invoke-virtual {v0, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v3
    instance-of v4, v3, Landroid/widget/TextView;
    if-eqz v4, :create_row
    check-cast v3, Landroid/widget/TextView;
    goto :style_row

    :create_row
    new-instance v3, Landroid/widget/TextView;
    invoke-direct {v3, p0}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V
    invoke-virtual {v3, v2}, Landroid/view/View;->setId(I)V
    invoke-virtual {v1}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup${'$'}LayoutParams;
    move-result-object v4
    const/4 v5, -0x1
    iget v4, v4, Landroid/view/ViewGroup${'$'}LayoutParams;->height:I
    new-instance v6, Landroid/widget/LinearLayout${'$'}LayoutParams;
    invoke-direct {v6, v5, v4}, Landroid/widget/LinearLayout${'$'}LayoutParams;-><init>(II)V
    invoke-virtual {v3, v6}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup${'$'}LayoutParams;)V

    invoke-virtual {v1}, Landroid/view/View;->getPaddingLeft()I
    move-result v4
    invoke-virtual {v1}, Landroid/view/View;->getPaddingRight()I
    move-result v5
    const/4 v6, 0x0
    invoke-virtual {v3, v4, v6, v5, v6}, Landroid/view/View;->setPadding(IIII)V
    invoke-virtual {v1}, Landroid/widget/TextView;->getGravity()I
    move-result v4
    invoke-virtual {v3, v4}, Landroid/widget/TextView;->setGravity(I)V
    invoke-virtual {v1}, Landroid/widget/TextView;->getCompoundDrawablePadding()I
    move-result v4
    invoke-virtual {v3, v4}, Landroid/widget/TextView;->setCompoundDrawablePadding(I)V
    const/4 v4, 0x2
    const/high16 v5, 0x41800000    # 16.0f
    invoke-virtual {v3, v4, v5}, Landroid/widget/TextView;->setTextSize(IF)V
    invoke-virtual {v3, v6}, Landroid/widget/TextView;->setIncludeFontPadding(Z)V
    const/4 v4, 0x1
    invoke-virtual {v3, v4}, Landroid/view/View;->setClickable(Z)V
    const-string v4, "\u0410\u043a\u043a\u0430\u0443\u043d\u0442\u044b"
    invoke-virtual {v3, v4}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    new-instance v4, Lru/fourpda/client/MultiAccount;
    invoke-direct {v4, p0, v6}, Lru/fourpda/client/MultiAccount;-><init>(Lru/fourpda/client/MainActivity;I)V
    invoke-virtual {v3, v4}, Landroid/view/View;->setOnClickListener(Landroid/view/View${'$'}OnClickListener;)V

    invoke-virtual {v0, v1}, Landroid/view/ViewGroup;->indexOfChild(Landroid/view/View;)I
    move-result v4
    if-ltz v4, :add_end
    invoke-virtual {v0, v3, v4}, Landroid/view/ViewGroup;->addView(Landroid/view/View;I)V
    goto :style_row
    :add_end
    invoke-virtual {v0, v3}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

    :style_row
    sget v4, Lru/fourpda/client/k1${'$'}a;->U:I
    invoke-virtual {v3, v4}, Landroid/widget/TextView;->setTextColor(I)V
    iget-object v4, p0, Lru/fourpda/client/MainActivity;->g:Lru/fourpda/client/k1;
    if-eqz v4, :show_row
    sget v5, Lru/fourpda/client/R${'$'}drawable;->nav_item:I
    invoke-virtual {v4, v5}, Lru/fourpda/client/k1;->f(I)Landroid/graphics/drawable/Drawable;
    move-result-object v5
    invoke-virtual {v3, v5}, Landroid/view/View;->setBackgroundDrawable(Landroid/graphics/drawable/Drawable;)V
    sget v5, Lru/fourpda/client/R${'$'}drawable;->ic_nav_profile:I
    invoke-virtual {v4, v5}, Lru/fourpda/client/k1;->f(I)Landroid/graphics/drawable/Drawable;
    move-result-object v5
    const/4 v6, 0x0
    invoke-virtual {v3, v5, v6, v6, v6}, Landroid/widget/TextView;->setCompoundDrawablesWithIntrinsicBounds(Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;Landroid/graphics/drawable/Drawable;)V

    :show_row
    const/4 v4, 0x0
    invoke-virtual {v3, v4}, Landroid/view/View;->setVisibility(I)V

    :done
    return-void
.end method

.method public static showMainDialog(Lru/fourpda/client/MainActivity;)V
    .registers 13
    invoke-static {p0}, Lru/fourpda/client/MultiAccount;->syncCurrent(Landroid/content/Context;)V
    invoke-static {p0}, Lru/fourpda/client/MultiAccount;->loadArray(Landroid/content/Context;)Lorg/json/JSONArray;
    move-result-object v0
    invoke-virtual {v0}, Lorg/json/JSONArray;->length()I
    move-result v1

    const/4 v2, 0x0
    sget-object v3, Lru/fourpda/client/F;->x:Lru/fourpda/client/F;
    if-eqz v3, :init_b1
    iget v2, v3, Lru/fourpda/client/F;->q:I

    :init_b1
    new-instance v3, Lru/fourpda/client/MultiAccount;
    const/4 v4, 0x1
    invoke-direct {v3, p0, v4}, Lru/fourpda/client/MultiAccount;-><init>(Lru/fourpda/client/MainActivity;I)V
    new-instance v5, Lru/fourpda/client/B1;
    invoke-direct {v5, p0, v3, v4}, Lru/fourpda/client/B1;-><init>(Lru/fourpda/client/MainActivity;Lru/fourpda/client/B1${'$'}b;Z)V

    const/4 v6, 0x0
    const/4 v7, 0x0
    const/4 v8, 0x0
    const-string v9, "\u0410\u043a\u043a\u0430\u0443\u043d\u0442\u044b"
    const/4 v10, 0x1
    const/4 v11, 0x0
    invoke-virtual/range {v5 .. v11}, Lru/fourpda/client/B1;->c(IIILjava/lang/String;ZZ)V

    const/4 v3, 0x0
    :loop
    if-ge v3, v1, :fill_actions
    invoke-virtual {v0, v3}, Lorg/json/JSONArray;->optJSONObject(I)Lorg/json/JSONObject;
    move-result-object v6
    const-string v7, "?"
    const/4 v8, 0x0
    if-eqz v6, :format_item
    const-string v9, "name"
    invoke-virtual {v6, v9, v7}, Lorg/json/JSONObject;->optString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v7
    const-string v9, "id"
    invoke-virtual {v6, v9, v8}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;I)I
    move-result v8

    :format_item
    new-instance v6, Ljava/lang/StringBuilder;
    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V
    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v7, " ["
    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v6, v8}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    const-string v7, "]"
    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v9

    const/4 v10, 0x0
    if-ne v8, v2, :add_acc_row
    if-lez v2, :add_acc_row
    const/4 v10, 0x1

    :add_acc_row
    const/4 v6, 0x1
    move v7, v3
    invoke-virtual/range {v5 .. v10}, Lru/fourpda/client/B1;->b(IIILjava/lang/String;Z)V
    add-int/lit8 v3, v3, 0x1
    goto :loop

    :fill_actions
    const/4 v6, 0x2
    const/4 v7, 0x1
    const/4 v8, 0x0
    const-string v9, "\u0412\u0432\u0435\u0441\u0442\u0438 ID \u0438 pass_hash"
    invoke-virtual {v5, v6, v7, v8, v9}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V

    const/4 v7, 0x2
    const-string v9, "\u0412\u043e\u0439\u0442\u0438 \u043f\u043e \u043b\u043e\u0433\u0438\u043d\u0443 \u0438 \u043f\u0430\u0440\u043e\u043b\u044e"
    invoke-virtual {v5, v6, v7, v8, v9}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V

    if-lez v1, :show_dlg
    const/4 v7, 0x3
    const-string v9, "\u0421\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u0442\u044c ID \u0438 pass_hash"
    invoke-virtual {v5, v6, v7, v8, v9}, Lru/fourpda/client/B1;->a(IIILjava/lang/String;)V

    const/4 v7, 0x4
    const-string v9, "\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u0430\u043a\u043a\u0430\u0443\u043d\u0442 \u0438\u0437 \u0441\u043f\u0438\u0441\u043a\u0430"
    const/4 v10, 0x0
    const/4 v11, 0x1
    invoke-virtual/range {v5 .. v11}, Lru/fourpda/client/B1;->c(IIILjava/lang/String;ZZ)V

    :show_dlg
    const/4 v0, 0x0
    invoke-virtual {v5, v0}, Lru/fourpda/client/B1;->e(Landroid/view/View;)Z
    return-void
.end method

.method public static switchTo(Lru/fourpda/client/MainActivity;ILjava/lang/String;Ljava/lang/String;Z)V
    .locals 6
    invoke-static {p0}, Lru/fourpda/client/MultiAccount;->syncCurrent(Landroid/content/Context;)V
    iget-object v0, p0, Lru/fourpda/client/MainActivity;->a:Lru/fourpda/client/MainLayout;
    const/4 v1, 0x0
    if-eqz v0, :prefs
    invoke-virtual {v0, v1}, Lru/fourpda/client/MainLayout;->p(Z)V

    :prefs
    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
    move-result-object v0
    const-string v2, "member_id"
    invoke-interface {v0, v2, p1}, Landroid/content/SharedPreferences${'$'}Editor;->putInt(Ljava/lang/String;I)Landroid/content/SharedPreferences${'$'}Editor;
    const-string v2, "login_key"
    invoke-interface {v0, v2, p2}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
    const-string v2, "member_hidden"
    invoke-interface {v0, v2, p4}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
    const-string v2, "member_info_id"
    invoke-interface {v0, v2, p1}, Landroid/content/SharedPreferences${'$'}Editor;->putInt(Ljava/lang/String;I)Landroid/content/SharedPreferences${'$'}Editor;
    const-string v2, "member_info_name"
    invoke-interface {v0, v2, p3}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
    const-string v2, "member_info_avatar"
    const-string v3, ""
    invoke-interface {v0, v2, v3}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
    const-string v2, "member_info_tickets"
    invoke-interface {v0, v2, v1}, Landroid/content/SharedPreferences${'$'}Editor;->putBoolean(Ljava/lang/String;Z)Landroid/content/SharedPreferences${'$'}Editor;
    invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->commit()Z

    sget-object v0, Lru/fourpda/client/F;->x:Lru/fourpda/client/F;
    if-eqz v0, :reset_badges
    iput p1, v0, Lru/fourpda/client/F;->q:I

    iget-object v2, v0, Lru/fourpda/client/F;->s:Lru/fourpda/client/m1;
    const/4 v3, 0x0
    if-eqz v2, :clear_tables
    invoke-virtual {v2}, Lru/fourpda/client/m1;->b()V
    iput-object v3, v0, Lru/fourpda/client/F;->s:Lru/fourpda/client/m1;

    :clear_tables
    iput-object v3, v0, Lru/fourpda/client/F;->r:Lru/fourpda/client/F${'$'}h;
    invoke-static {}, Lru/fourpda/client/v;->E()V
    const/4 v2, 0x2
    invoke-static {v2, v1}, Lru/fourpda/client/v;->s(II)V
    invoke-static {v1}, Lru/fourpda/client/v;->b(Z)V

    :reset_badges
    sget v2, Lru/fourpda/client/e1;->c3:I
    invoke-virtual {p0, v2}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v2
    if-eqz v2, :reset_qms
    const/16 v4, 0x8
    invoke-virtual {v2, v4}, Landroid/view/View;->setVisibility(I)V

    :reset_qms
    sget v2, Lru/fourpda/client/e1;->e3:I
    invoke-virtual {p0, v2}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v2
    if-eqz v2, :reset_men
    const/16 v4, 0x8
    invoke-virtual {v2, v4}, Landroid/view/View;->setVisibility(I)V

    :reset_men
    sget v2, Lru/fourpda/client/e1;->d3:I
    invoke-virtual {p0, v2}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v2
    if-eqz v2, :reconnect
    const/16 v4, 0x8
    invoke-virtual {v2, v4}, Landroid/view/View;->setVisibility(I)V

    :reconnect
    if-eqz v0, :toast
    const/4 v3, 0x0
    iget-object v2, v0, Lru/fourpda/client/F;->m:Lru/fourpda/client/F${'$'}d;
    const/16 v4, 0xd
    invoke-virtual {v2, v4}, Landroid/os/Handler;->removeMessages(I)V
    sget-object v4, Lru/fourpda/client/F;->E:Lru/fourpda/client/s1${'$'}b;
    invoke-virtual {v4, v3}, Lru/fourpda/client/s1${'$'}b;->c(Ljava/lang/Object;)Z
    invoke-virtual {v0}, Lru/fourpda/client/F;->f0()V
    const/16 v3, 0x9
    invoke-virtual {v2, v3}, Landroid/os/Handler;->sendEmptyMessage(I)Z
    invoke-virtual {v0}, Lru/fourpda/client/n0;->y()Z
    move-result v2
    if-nez v2, :post_reload
    invoke-virtual {v0}, Lru/fourpda/client/n0;->t()V

    :post_reload
    iget-object v0, p0, Lru/fourpda/client/MainActivity;->a:Lru/fourpda/client/MainLayout;
    if-eqz v0, :toast
    new-instance v2, Lru/fourpda/client/MultiAccount;
    const/4 v3, 0x4
    invoke-direct {v2, p0, v3}, Lru/fourpda/client/MultiAccount;-><init>(Lru/fourpda/client/MainActivity;I)V
    const-wide/16 v3, 0x2bc
    invoke-virtual {v0, v2, v3, v4}, Landroid/view/View;->postDelayed(Ljava/lang/Runnable;J)Z

    :toast
    new-instance v0, Ljava/lang/StringBuilder;
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
    const-string v2, "\u0410\u043a\u043a\u0430\u0443\u043d\u0442: "
    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v0, p3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0
    invoke-static {p0, v0, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    return-void
.end method

.method public static startPasswordLogin(Lru/fourpda/client/MainActivity;)V
    .registers 5
    invoke-static {p0}, Lru/fourpda/client/MultiAccount;->syncCurrent(Landroid/content/Context;)V
    iget-object v0, p0, Lru/fourpda/client/MainActivity;->a:Lru/fourpda/client/MainLayout;
    const/4 v1, 0x0
    if-eqz v0, :clear_prefs
    invoke-virtual {v0, v1}, Lru/fourpda/client/MainLayout;->p(Z)V

    :clear_prefs
    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
    move-result-object v0
    const-string v2, "member_id"
    invoke-interface {v0, v2}, Landroid/content/SharedPreferences${'$'}Editor;->remove(Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
    const-string v2, "login_key"
    invoke-interface {v0, v2}, Landroid/content/SharedPreferences${'$'}Editor;->remove(Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
    const-string v2, "member_info_id"
    invoke-interface {v0, v2}, Landroid/content/SharedPreferences${'$'}Editor;->remove(Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
    invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->commit()Z

    sget-object v0, Lru/fourpda/client/F;->x:Lru/fourpda/client/F;
    if-eqz v0, :open_login
    iput v1, v0, Lru/fourpda/client/F;->q:I
    const/4 v2, 0x0
    iput-object v2, v0, Lru/fourpda/client/F;->r:Lru/fourpda/client/F${'$'}h;
    iget-object v3, v0, Lru/fourpda/client/F;->s:Lru/fourpda/client/m1;
    if-eqz v3, :reset_sync
    invoke-virtual {v3}, Lru/fourpda/client/m1;->b()V
    iput-object v2, v0, Lru/fourpda/client/F;->s:Lru/fourpda/client/m1;

    :reset_sync
    iget-object v3, v0, Lru/fourpda/client/F;->m:Lru/fourpda/client/F${'$'}d;
    const/16 v1, 0xd
    invoke-virtual {v3, v1}, Landroid/os/Handler;->removeMessages(I)V
    const/4 v1, 0x0
    invoke-static {v1}, Lru/fourpda/client/v;->b(Z)V
    sget-object v3, Lru/fourpda/client/F;->E:Lru/fourpda/client/s1${'$'}b;
    invoke-virtual {v3, v2}, Lru/fourpda/client/s1${'$'}b;->c(Ljava/lang/Object;)Z
    iget-object v2, v0, Lru/fourpda/client/F;->m:Lru/fourpda/client/F${'$'}d;
    const/16 v3, 0x9
    invoke-virtual {v2, v3}, Landroid/os/Handler;->sendEmptyMessage(I)Z
    invoke-static {}, Lru/fourpda/client/MultiAccount;->safeDisconnect()V

    :open_login
    iget-object v0, p0, Lru/fourpda/client/MainActivity;->a:Lru/fourpda/client/MainLayout;
    if-eqz v0, :done
    iget-object v0, v0, Lru/fourpda/client/MainLayout;->y:Lru/fourpda/client/l1;
    if-eqz v0, :done
    new-instance v2, Lru/fourpda/client/E0;
    invoke-direct {v2, p0, v1}, Lru/fourpda/client/E0;-><init>(Lru/fourpda/client/MainActivity;Z)V
    invoke-virtual {v0, v2}, Lru/fourpda/client/l1;->l(Lru/fourpda/client/w0;)V

    :done
    return-void
.end method

.method public static copyCurrentToken(Lru/fourpda/client/MainActivity;)V
    .registers 6
    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    const-string v1, "member_id"
    const/4 v2, 0x0
    invoke-interface {v0, v1, v2}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I
    move-result v1
    const-string v3, "login_key"
    const-string v4, ""
    invoke-interface {v0, v3, v4}, Landroid/content/SharedPreferences;->getString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v0
    if-lez v1, :no_active
    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z
    move-result v3
    if-nez v3, :no_active

    new-instance v3, Ljava/lang/StringBuilder;
    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V
    const-string v4, "member_id="
    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    const-string v1, "; pass_hash="
    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v0

    const-string v1, "clipboard"
    invoke-virtual {p0, v1}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
    move-result-object v1
    check-cast v1, Landroid/content/ClipboardManager;
    const-string v3, "4PDA Session"
    invoke-static {v3, v0}, Landroid/content/ClipData;->newPlainText(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Landroid/content/ClipData;
    move-result-object v0
    invoke-virtual {v1, v0}, Landroid/content/ClipboardManager;->setPrimaryClip(Landroid/content/ClipData;)V
    const-string v0, "\u0421\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d\u043e: member_id \u0438 pass_hash"
    invoke-static {p0, v0, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    return-void

    :no_active
    const-string v0, "\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0432\u043e\u0439\u0434\u0438\u0442\u0435 \u0432 \u0430\u043a\u043a\u0430\u0443\u043d\u0442"
    invoke-static {p0, v0, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    return-void
.end method

.method public static showTokenDialog(Lru/fourpda/client/MainActivity;)V
    .registers 8
    new-instance v0, Lru/fourpda/client/C1;
    const-string v2, "\u0412\u0445\u043e\u0434 \u043f\u043e ID \u0438 pass_hash"
    const/4 v3, 0x0
    const-string v4, "\u0412\u041e\u0419\u0422\u0418"
    const/4 v5, 0x0
    move-object v1, p0
    invoke-direct/range {v0 .. v5}, Lru/fourpda/client/C1;-><init>(Lru/fourpda/client/MainActivity;Ljava/lang/String;ZLjava/lang/String;Ljava/lang/String;)V

    iget-object v1, v0, Lru/fourpda/client/C1;->k:Landroid/widget/TextView;
    const-string v2, "\u0423\u043a\u0430\u0436\u0438\u0442\u0435 member_id \u0438 32-\u0441\u0438\u043c\u0432\u043e\u043b\u044c\u043d\u044b\u0439 pass_hash (\u0438\u043b\u0438 \u0432\u0441\u0442\u0430\u0432\u044c\u0442\u0435 \u0441\u0442\u0440\u043e\u043a\u0443 \u043a\u0443\u043a\u0438 \u0432 \u043f\u0435\u0440\u0432\u043e\u0435 \u043f\u043e\u043b\u0435):"
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    iget-object v1, v0, Lru/fourpda/client/C1;->l:Landroid/widget/EditText;
    const/4 v2, 0x1
    invoke-virtual {v1, v2}, Landroid/widget/TextView;->setInputType(I)V
    sget v3, Lru/fourpda/client/k1${'$'}a;->c0:I
    invoke-virtual {v1, v3}, Landroid/widget/TextView;->setHintTextColor(I)V
    const-string v4, "member_id (\u0438\u043b\u0438 \u0441\u0442\u0440\u043e\u043a\u0430 \u0441 pass_hash)"
    invoke-virtual {v1, v4}, Landroid/widget/TextView;->setHint(Ljava/lang/CharSequence;)V

    invoke-virtual {p0}, Landroid/app/Activity;->getLayoutInflater()Landroid/view/LayoutInflater;
    move-result-object v4
    sget v5, Lru/fourpda/client/f1;->F:I
    const/4 v6, 0x0
    invoke-virtual {v4, v5, v6}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;)Landroid/view/View;
    move-result-object v4
    sget v5, Lru/fourpda/client/e1;->e6:I
    invoke-virtual {v4, v5}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v5
    check-cast v5, Landroid/widget/EditText;
    check-cast v4, Landroid/view/ViewGroup;
    invoke-virtual {v4, v5}, Landroid/view/ViewGroup;->removeView(Landroid/view/View;)V
    invoke-virtual {v5, v2}, Landroid/widget/TextView;->setInputType(I)V
    invoke-virtual {v5, v3}, Landroid/widget/TextView;->setHintTextColor(I)V
    const-string v3, "pass_hash (32 \u0441\u0438\u043c\u0432\u043e\u043b\u0430, \u0435\u0441\u043b\u0438 \u043e\u0442\u0434\u0435\u043b\u044c\u043d\u043e)"
    invoke-virtual {v5, v3}, Landroid/widget/TextView;->setHint(Ljava/lang/CharSequence;)V

    invoke-virtual {v5}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup${'$'}LayoutParams;
    move-result-object v3
    instance-of v4, v3, Landroid/widget/LinearLayout${'$'}LayoutParams;
    if-eqz v4, :add_edit2
    check-cast v3, Landroid/widget/LinearLayout${'$'}LayoutParams;
    iget v4, p0, Lru/fourpda/client/MainActivity;->b:F
    const/high16 v6, 0x41000000    # 8.0f
    mul-float/2addr v4, v6
    float-to-int v4, v4
    iput v4, v3, Landroid/widget/LinearLayout${'$'}LayoutParams;->topMargin:I

    :add_edit2
    iget-object v3, v0, Lru/fourpda/client/v1;->h:Landroid/view/View;
    check-cast v3, Landroid/view/ViewGroup;
    const/4 v4, 0x3
    invoke-virtual {v3, v5, v4}, Landroid/view/ViewGroup;->addView(Landroid/view/View;I)V

    iget-object v3, v0, Lru/fourpda/client/C1;->m:Lru/fourpda/client/Widgets${'$'}CheckboxTextView;
    const-string v4, "\u0421\u043a\u0440\u044b\u0442\u044b\u0439 \u0432\u0445\u043e\u0434"
    invoke-virtual {v3, v4}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    invoke-virtual {v3, v2}, Lru/fourpda/client/Widgets${'$'}CheckboxTextView;->setChecked(Z)V
    const/4 v4, 0x0
    invoke-virtual {v3, v4}, Landroid/view/View;->setVisibility(I)V

    new-instance v3, Lru/fourpda/client/MultiAccount;
    const/4 v4, 0x2
    invoke-direct {v3, p0, v4}, Lru/fourpda/client/MultiAccount;-><init>(Lru/fourpda/client/MainActivity;I)V
    iput-object v0, v3, Lru/fourpda/client/MultiAccount;->dlg:Lru/fourpda/client/C1;
    iput-object v5, v3, Lru/fourpda/client/MultiAccount;->edit2:Landroid/widget/EditText;
    invoke-virtual {v0, v3, v2}, Lru/fourpda/client/v1;->g(Landroid/view/View${'$'}OnClickListener;Z)V
    invoke-virtual {v0, v2, v2, v2}, Lru/fourpda/client/v1;->c(ZZZ)V
    return-void
.end method

.method public static onTokenSubmit(Lru/fourpda/client/MainActivity;Landroid/widget/EditText;Landroid/widget/EditText;Lru/fourpda/client/Widgets${'$'}CheckboxTextView;)V
    .registers 11
    invoke-virtual {p1}, Landroid/widget/EditText;->getText()Landroid/text/Editable;
    move-result-object v0
    invoke-virtual {v0}, Ljava/lang/Object;->toString()Ljava/lang/String;
    move-result-object v0
    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;
    move-result-object v0

    invoke-virtual {p2}, Landroid/widget/EditText;->getText()Landroid/text/Editable;
    move-result-object v1
    invoke-virtual {v1}, Ljava/lang/Object;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-virtual {v1}, Ljava/lang/String;->trim()Ljava/lang/String;
    move-result-object v1

    new-instance v2, Ljava/lang/StringBuilder;
    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V
    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v3, " "
    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v2

    const-string v3, "([a-fA-F0-9]{32})"
    invoke-static {v3}, Ljava/util/regex/Pattern;->compile(Ljava/lang/String;)Ljava/util/regex/Pattern;
    move-result-object v3
    invoke-virtual {v3, v2}, Ljava/util/regex/Pattern;->matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;
    move-result-object v3
    invoke-virtual {v3}, Ljava/util/regex/Matcher;->find()Z
    move-result v4
    const/4 v5, 0x0
    if-eqz v4, :bad_input
    const/4 v4, 0x1
    invoke-virtual {v3, v4}, Ljava/util/regex/Matcher;->group(I)Ljava/lang/String;
    move-result-object v1

    const-string v3, ""
    invoke-virtual {v2, v1, v3}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;
    move-result-object v2
    const-string v3, "(\\d{1,10})"
    invoke-static {v3}, Ljava/util/regex/Pattern;->compile(Ljava/lang/String;)Ljava/util/regex/Pattern;
    move-result-object v3
    invoke-virtual {v3, v2}, Ljava/util/regex/Pattern;->matcher(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;
    move-result-object v3
    invoke-virtual {v3}, Ljava/util/regex/Matcher;->find()Z
    move-result v2
    if-eqz v2, :bad_input
    invoke-virtual {v3, v4}, Ljava/util/regex/Matcher;->group(I)Ljava/lang/String;
    move-result-object v0

    :try_start
    invoke-static {v0}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I
    move-result v2
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :bad_input
    if-lez v2, :bad_input

    invoke-virtual {p3}, Lru/fourpda/client/Widgets${'$'}CheckboxTextView;->getChecked()Z
    move-result v3
    new-instance v4, Ljava/lang/StringBuilder;
    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V
    const-string v5, "ID "
    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v4

    invoke-static {p0, v2, v1, v4, v3}, Lru/fourpda/client/MultiAccount;->saveAccount(Landroid/content/Context;ILjava/lang/String;Ljava/lang/String;Z)V
    invoke-static {p0, v2, v1, v4, v3}, Lru/fourpda/client/MultiAccount;->switchTo(Lru/fourpda/client/MainActivity;ILjava/lang/String;Ljava/lang/String;Z)V
    return-void

    :bad_input
    const-string v0, "\u0423\u043a\u0430\u0436\u0438\u0442\u0435 member_id \u0438 32-\u0441\u0438\u043c\u0432\u043e\u043b\u044c\u043d\u044b\u0439 pass_hash"
    invoke-static {p0, v0, v5}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    return-void
.end method

.method public static showDeleteDialog(Lru/fourpda/client/MainActivity;)V
    .registers 13
    invoke-static {p0}, Lru/fourpda/client/MultiAccount;->loadArray(Landroid/content/Context;)Lorg/json/JSONArray;
    move-result-object v0
    invoke-virtual {v0}, Lorg/json/JSONArray;->length()I
    move-result v1
    if-lez v1, :done

    new-instance v2, Lru/fourpda/client/MultiAccount;
    const/4 v3, 0x3
    invoke-direct {v2, p0, v3}, Lru/fourpda/client/MultiAccount;-><init>(Lru/fourpda/client/MainActivity;I)V
    new-instance v5, Lru/fourpda/client/B1;
    const/4 v4, 0x1
    invoke-direct {v5, p0, v2, v4}, Lru/fourpda/client/B1;-><init>(Lru/fourpda/client/MainActivity;Lru/fourpda/client/B1${'$'}b;Z)V

    const/4 v6, 0x0
    const/4 v7, 0x0
    const/4 v8, 0x0
    const-string v9, "\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u0438\u0437 \u0441\u043f\u0438\u0441\u043a\u0430"
    const/4 v10, 0x1
    const/4 v11, 0x0
    invoke-virtual/range {v5 .. v11}, Lru/fourpda/client/B1;->c(IIILjava/lang/String;ZZ)V

    const/4 v3, 0x0
    :loop
    if-ge v3, v1, :show
    invoke-virtual {v0, v3}, Lorg/json/JSONArray;->optJSONObject(I)Lorg/json/JSONObject;
    move-result-object v4
    const-string v6, "?"
    const/4 v7, 0x0
    if-eqz v4, :fmt
    const-string v8, "name"
    invoke-virtual {v4, v8, v6}, Lorg/json/JSONObject;->optString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    move-result-object v6
    const-string v8, "id"
    invoke-virtual {v4, v8, v7}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;I)I
    move-result v7

    :fmt
    new-instance v4, Ljava/lang/StringBuilder;
    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V
    invoke-virtual {v4, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    const-string v6, " ["
    invoke-virtual {v4, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v4, v7}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    const-string v6, "]"
    invoke-virtual {v4, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v9

    const/4 v6, 0x3
    move v7, v3
    const/4 v8, 0x0
    const/4 v10, 0x0
    const/4 v11, 0x1
    invoke-virtual/range {v5 .. v11}, Lru/fourpda/client/B1;->c(IIILjava/lang/String;ZZ)V
    add-int/lit8 v3, v3, 0x1
    goto :loop

    :show
    const/4 v0, 0x0
    invoke-virtual {v5, v0}, Lru/fourpda/client/B1;->e(Landroid/view/View;)Z
    :done
    return-void
.end method

.method public static deleteAt(Lru/fourpda/client/MainActivity;I)V
    .registers 7
    invoke-static {p0}, Lru/fourpda/client/MultiAccount;->loadArray(Landroid/content/Context;)Lorg/json/JSONArray;
    move-result-object v0
    new-instance v1, Lorg/json/JSONArray;
    invoke-direct {v1}, Lorg/json/JSONArray;-><init>()V
    invoke-virtual {v0}, Lorg/json/JSONArray;->length()I
    move-result v2
    const/4 v3, 0x0

    :loop
    if-ge v3, v2, :save
    if-eq v3, p1, :skip
    invoke-virtual {v0, v3}, Lorg/json/JSONArray;->optJSONObject(I)Lorg/json/JSONObject;
    move-result-object v4
    if-eqz v4, :skip
    invoke-virtual {v1, v4}, Lorg/json/JSONArray;->put(Ljava/lang/Object;)Lorg/json/JSONArray;
    :skip
    add-int/lit8 v3, v3, 0x1
    goto :loop

    :save
    invoke-static {p0}, Landroid/preference/PreferenceManager;->getDefaultSharedPreferences(Landroid/content/Context;)Landroid/content/SharedPreferences;
    move-result-object v0
    invoke-interface {v0}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences${'$'}Editor;
    move-result-object v0
    const-string v2, "rv_multi_accounts"
    invoke-virtual {v1}, Lorg/json/JSONArray;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-interface {v0, v2, v1}, Landroid/content/SharedPreferences${'$'}Editor;->putString(Ljava/lang/String;Ljava/lang/String;)Landroid/content/SharedPreferences${'$'}Editor;
    invoke-interface {v0}, Landroid/content/SharedPreferences${'$'}Editor;->commit()Z
    const-string v0, "\u0410\u043a\u043a\u0430\u0443\u043d\u0442 \u0443\u0434\u0430\u043b\u0451\u043d \u0438\u0437 \u0441\u043f\u0438\u0441\u043a\u0430"
    const/4 v1, 0x0
    invoke-static {p0, v0, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
    move-result-object v0
    invoke-virtual {v0}, Landroid/widget/Toast;->show()V
    return-void
.end method

.method public onClick(Landroid/view/View;)V
    .registers 6
    iget-object v0, p0, Lru/fourpda/client/MultiAccount;->act:Lru/fourpda/client/MainActivity;
    if-eqz v0, :done
    iget v1, p0, Lru/fourpda/client/MultiAccount;->mode:I
    const/4 v2, 0x2
    if-ne v1, v2, :open_main
    iget-object v1, p0, Lru/fourpda/client/MultiAccount;->dlg:Lru/fourpda/client/C1;
    if-eqz v1, :done
    iget-object v2, v1, Lru/fourpda/client/C1;->l:Landroid/widget/EditText;
    iget-object v3, p0, Lru/fourpda/client/MultiAccount;->edit2:Landroid/widget/EditText;
    iget-object v1, v1, Lru/fourpda/client/C1;->m:Lru/fourpda/client/Widgets${'$'}CheckboxTextView;
    invoke-static {v0, v2, v3, v1}, Lru/fourpda/client/MultiAccount;->onTokenSubmit(Lru/fourpda/client/MainActivity;Landroid/widget/EditText;Landroid/widget/EditText;Lru/fourpda/client/Widgets${'$'}CheckboxTextView;)V
    return-void

    :open_main
    invoke-static {v0}, Lru/fourpda/client/MultiAccount;->showMainDialog(Lru/fourpda/client/MainActivity;)V
    :done
    return-void
.end method

.method public onLongClick(Landroid/view/View;)Z
    .registers 3
    iget-object v0, p0, Lru/fourpda/client/MultiAccount;->act:Lru/fourpda/client/MainActivity;
    if-eqz v0, :done
    invoke-static {v0}, Lru/fourpda/client/MultiAccount;->showMainDialog(Lru/fourpda/client/MainActivity;)V
    const/4 v0, 0x1
    return v0
    :done
    const/4 v0, 0x0
    return v0
.end method

.method public a(III)V
    .registers 10
    iget-object v0, p0, Lru/fourpda/client/MultiAccount;->act:Lru/fourpda/client/MainActivity;
    if-eqz v0, :done
    const/4 v1, 0x1
    if-ne p1, v1, :check_actions

    invoke-static {v0}, Lru/fourpda/client/MultiAccount;->loadArray(Landroid/content/Context;)Lorg/json/JSONArray;
    move-result-object v1
    invoke-virtual {v1, p2}, Lorg/json/JSONArray;->optJSONObject(I)Lorg/json/JSONObject;
    move-result-object v1
    if-eqz v1, :done
    const-string v2, "id"
    invoke-virtual {v1, v2}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;)I
    move-result v2
    const-string v3, "key"
    invoke-virtual {v1, v3}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v3
    const-string v4, "name"
    invoke-virtual {v1, v4}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
    move-result-object v4
    const-string v5, "hidden"
    invoke-virtual {v1, v5}, Lorg/json/JSONObject;->optBoolean(Ljava/lang/String;)Z
    move-result v5
    invoke-static {v0, v2, v3, v4, v5}, Lru/fourpda/client/MultiAccount;->switchTo(Lru/fourpda/client/MainActivity;ILjava/lang/String;Ljava/lang/String;Z)V
    return-void

    :check_actions
    const/4 v2, 0x2
    if-ne p1, v2, :check_del
    if-ne p2, v1, :act_pwd
    invoke-static {v0}, Lru/fourpda/client/MultiAccount;->showTokenDialog(Lru/fourpda/client/MainActivity;)V
    return-void
    :act_pwd
    if-ne p2, v2, :act_copy
    invoke-static {v0}, Lru/fourpda/client/MultiAccount;->startPasswordLogin(Lru/fourpda/client/MainActivity;)V
    return-void
    :act_copy
    const/4 v1, 0x3
    if-ne p2, v1, :act_del
    invoke-static {v0}, Lru/fourpda/client/MultiAccount;->copyCurrentToken(Lru/fourpda/client/MainActivity;)V
    return-void
    :act_del
    invoke-static {v0}, Lru/fourpda/client/MultiAccount;->showDeleteDialog(Lru/fourpda/client/MainActivity;)V
    return-void

    :check_del
    const/4 v1, 0x3
    if-ne p1, v1, :done
    invoke-static {v0, p2}, Lru/fourpda/client/MultiAccount;->deleteAt(Lru/fourpda/client/MainActivity;I)V

    :done
    return-void
.end method

.method public run()V
    .registers 5
    iget-object v0, p0, Lru/fourpda/client/MultiAccount;->act:Lru/fourpda/client/MainActivity;
    if-eqz v0, :done
    iget-object v1, v0, Lru/fourpda/client/MainActivity;->a:Lru/fourpda/client/MainLayout;
    if-eqz v1, :done
    iget-object v1, v1, Lru/fourpda/client/MainLayout;->y:Lru/fourpda/client/l1;
    if-eqz v1, :done
    iget-object v2, v1, Lru/fourpda/client/l1;->b:Lru/fourpda/client/w0;
    if-eqz v2, :done
    instance-of v3, v2, Lru/fourpda/client/E0;
    if-eqz v3, :reload_tab
    new-instance v2, Lru/fourpda/client/S0;
    invoke-direct {v2, v0}, Lru/fourpda/client/S0;-><init>(Lru/fourpda/client/MainActivity;)V
    invoke-virtual {v1, v2}, Lru/fourpda/client/l1;->l(Lru/fourpda/client/w0;)V
    invoke-virtual {v1}, Lru/fourpda/client/l1;->d()V
    return-void
    :reload_tab
    invoke-virtual {v2}, Lru/fourpda/client/w0;->z()V
    :done
    return-void
.end method
"""

val multiAccountPatch = bytecodePatch(
    name = "Мультиаккаунт",
    description = "Переключение между несколькими аккаунтами в левой шторке (пункт «Аккаунты» в меню профиля и долгий тап по шапке профиля) с поддержкой входа по member_id и pass_hash без капчи и без сброса сессий.",
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        if (classDefs[MULTI_ACCOUNT] == null) {
            classDefs += assemble(MULTI_ACCOUNT_SMALI)
        }

        val profileCallback = classDefs[PROFILE_CALLBACK]
            ?: error("не найден обработчик профиля MainActivity\$u")
        val updateProfile = proxy(profileCallback).mutableClass.methods.firstOrNull { method ->
            method.name == "b" && method.parameterTypes.size == 1
        } ?: error("не найден метод обновления профиля MainActivity\$u.b")

        updateProfile.addInstructions(
            0,
            """
            iget-object v0, p0, Lru/fourpda/client/MainActivity${'$'}u;->a:Lru/fourpda/client/MainActivity;
            invoke-static {v0, p1}, $MULTI_ACCOUNT->onProfile(Lru/fourpda/client/MainActivity;Lru/fourpda/client/F${'$'}h;)V
            """.trimIndent(),
        )

        val sessionCore = classDefs[SESSION_CORE]
            ?: error("не найден класс сессии F")
        val logoutMethod = proxy(sessionCore).mutableClass.methods.firstOrNull { method ->
            method.name == "g0" && method.parameterTypes.isEmpty()
        } ?: error("не найден метод выхода F.g0")

        val instructions = logoutMethod.implementation!!.instructions
        val initIdx = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.INVOKE_DIRECT &&
                instruction.calledMethod()?.definingClass == AUTH_PACKET &&
                instruction.calledMethod()?.name == "<init>"
        }
        if (initIdx >= 0) {
            val sendIdx = (initIdx + 1 until instructions.size).firstOrNull { idx ->
                val called = instructions[idx].calledMethod()
                called?.definingClass == SESSION_CORE && called.name == "m0"
            } ?: error("не найдена отправка пакета выхода в F.g0")
            logoutMethod.replaceInstruction(
                sendIdx,
                "invoke-static {}, $MULTI_ACCOUNT->safeDisconnect()V",
            )
        }
    }
}

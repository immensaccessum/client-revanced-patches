package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.removeInstructions
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
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

private const val TAB = "Lru/fourpda/client/l1;"
private const val LAYOUT = "Lru/fourpda/client/MainLayout;"
private const val MAIN_ACTIVITY = "Lru/fourpda/client/MainActivity;"
private const val RES_IDS = "Lru/fourpda/client/e1;"
private const val MOVE_TICKETS = "Lru/fourpda/client/MoveTicketsTab;"

// В nav_tabs восемь постоянных строк: семь пунктов меню (включая Тикеты) и подпись «Открытые вкладки».
private const val OPEN_TABS = 8

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
        error("ошибка сборки MoveTicketsTab")
    }
    val walker = smaliTreeWalker(
        CommonTreeNodeStream(fileTree.tree).apply { tokenStream = tokens },
    )
    walker.setDexBuilder(DexBuilder(Opcodes.getDefault()))
    return walker.smali_file()
}

private const val MOVE_TICKETS_SMALI = """
.class public Lru/fourpda/client/MoveTicketsTab;
.super Ljava/lang/Object;

.method public constructor <init>()V
    .registers 1
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static move(Landroid/view/View;)V
    .registers 4
    :try_start_0
    if-eqz p0, :catchall_0

    invoke-virtual {p0}, Landroid/view/View;->getParent()Landroid/view/ViewParent;
    move-result-object v0
    instance-of v1, v0, Landroid/view/ViewGroup;
    if-eqz v1, :catchall_0
    check-cast v0, Landroid/view/ViewGroup;
    invoke-virtual {v0, p0}, Landroid/view/ViewGroup;->removeView(Landroid/view/View;)V

    invoke-virtual {p0}, Landroid/view/View;->getRootView()Landroid/view/View;
    move-result-object v0
    if-eqz v0, :catchall_0

    sget v1, Lru/fourpda/client/e1;->a3:I
    invoke-virtual {v0, v1}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v1
    instance-of v2, v1, Landroid/view/ViewGroup;
    if-eqz v2, :catchall_0
    check-cast v1, Landroid/view/ViewGroup;

    sget v2, Lru/fourpda/client/e1;->V2:I
    invoke-virtual {v0, v2}, Landroid/view/View;->findViewById(I)Landroid/view/View;
    move-result-object v0
    if-eqz v0, :add_end

    invoke-virtual {v1, v0}, Landroid/view/ViewGroup;->indexOfChild(Landroid/view/View;)I
    move-result v0
    if-ltz v0, :add_end

    add-int/lit8 v0, v0, 0x1
    invoke-virtual {v1, p0, v0}, Landroid/view/ViewGroup;->addView(Landroid/view/View;I)V
    return-void

    :add_end
    invoke-virtual {v1, p0}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    :catchall_0
    return-void
.end method
"""

val newTabOnTopPatch = bytecodePatch(
    name = "Новая вкладка сверху и тикеты в шторке",
    description = "В левой шторке новая вкладка появляется сразу под «Открытые вкладки», а кнопка «Тикеты» переносится из подменю профиля под «Упоминания».",
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        // 1. Новая вкладка сразу под «Открытые вкладки» (индекс 8)
        val tab = classDefs[TAB] ?: error("в apk нет вкладки")
        val tabSetup = proxy(tab).mutableClass.methods.firstOrNull { method ->
            method.name == "<init>" && method.implementation?.instructions?.any { instruction ->
                instruction.field()?.let { it.definingClass == LAYOUT && it.name == "h" } == true
            } == true
        } ?: error("не найден конструктор вкладки")
        val tabInstructions = tabSetup.implementation!!.instructions
        val insert = tabInstructions.indices.firstOrNull { index ->
            if (index < 2) return@firstOrNull false
            val called = tabInstructions[index].calledMethod() ?: return@firstOrNull false
            val drawer = tabInstructions[index - 2].field()
            val row = tabInstructions[index - 1].field()
            called.name == "addView" && called.parameterTypes.size == 1 &&
                drawer?.definingClass == LAYOUT && drawer.name == "h" &&
                row?.definingClass == TAB && row.name == "k"
        } ?: error("не найдена вставка строки шторки")
        val call = tabInstructions[insert] as FiveRegisterInstruction
        val drawerRegister = call.registerC
        val rowRegister = call.registerD
        val indexRegister = 4
        if (
            drawerRegister > 15 || rowRegister > 15 ||
            drawerRegister == indexRegister || rowRegister == indexRegister
        ) {
            error("строка вкладки попала в широкий регистр")
        }
        tabSetup.removeInstructions(insert, 1)
        tabSetup.addInstructions(
            insert,
            """
            const/4 v$indexRegister, 0x$OPEN_TABS
            invoke-virtual {v$drawerRegister, v$rowRegister, v$indexRegister}, Landroid/view/ViewGroup;->addView(Landroid/view/View;I)V
            """.trimIndent(),
        )

        // 2. Перенос кнопки «Тикеты» из подменю профиля под «Упоминания»
        if (classDefs[MOVE_TICKETS] == null) {
            classDefs += assemble(MOVE_TICKETS_SMALI)
        }

        val mainActivity = classDefs[MAIN_ACTIVITY] ?: error("в apk нет MainActivity")
        val mainSetup = proxy(mainActivity).mutableClass.methods.firstOrNull { method ->
            method.name == "f" && method.parameterTypes == listOf("Landroid/os/Bundle;")
        } ?: error("не найден MainActivity.f")

        val mainInstructions = mainSetup.implementation!!.instructions
        val targetIndex = mainInstructions.indices.firstOrNull { index ->
            if (index < 5) return@firstOrNull false
            val called = mainInstructions[index].calledMethod()
            val sget = mainInstructions[index - 5].field()
            called?.name == "setOnClickListener" &&
                sget?.definingClass == RES_IDS && sget.name == "p3"
        } ?: error("не найдена настройка nav_user_tickets")

        val ticketsCall = mainInstructions[targetIndex] as FiveRegisterInstruction
        val viewRegister = ticketsCall.registerC
        if (viewRegister > 15) {
            error("кнопка тикетов попала в широкий регистр")
        }

        mainSetup.addInstructions(
            targetIndex + 1,
            """
            invoke-static {v$viewRegister}, $MOVE_TICKETS->move(Landroid/view/View;)V
            """.trimIndent(),
        )
    }
}

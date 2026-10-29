package app.revanced.patches.fourpda

import app.revanced.patcher.extensions.addInstructions
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

private const val MAIN_ACTIVITY = "Lru/fourpda/client/MainActivity;"
private const val RES_IDS = "Lru/fourpda/client/e1;"
private const val MOVE_TICKETS = "Lru/fourpda/client/MoveTicketsTab;"

private fun Instruction.calledMethod(): MethodReference? =
    (this as? ReferenceInstruction)?.reference as? MethodReference

private fun Instruction.fieldRef(): FieldReference? =
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

.method public static move(Lru/fourpda/client/MainActivity;Landroid/view/View;)V
    .registers 4
    if-eqz p0, :cond_return
    if-eqz p1, :cond_return

    invoke-virtual {p1}, Landroid/view/View;->getParent()Landroid/view/ViewParent;
    move-result-object v0
    instance-of v1, v0, Landroid/view/ViewGroup;
    if-eqz v1, :check_tabs
    check-cast v0, Landroid/view/ViewGroup;
    invoke-virtual {v0, p1}, Landroid/view/ViewGroup;->removeView(Landroid/view/View;)V

    :check_tabs
    iget-object v0, p0, Lru/fourpda/client/MainActivity;->a:Lru/fourpda/client/MainLayout;
    if-eqz v0, :cond_return
    iget-object v0, v0, Lru/fourpda/client/MainLayout;->h:Landroid/widget/LinearLayout;
    if-eqz v0, :cond_return

    sget v1, Lru/fourpda/client/e1;->V2:I
    invoke-virtual {p0, v1}, Landroid/app/Activity;->findViewById(I)Landroid/view/View;
    move-result-object v1
    if-eqz v1, :add_end

    invoke-virtual {v0, v1}, Landroid/view/ViewGroup;->indexOfChild(Landroid/view/View;)I
    move-result v1
    if-ltz v1, :add_end

    add-int/lit8 v1, v1, 0x1
    invoke-virtual {v0, p1, v1}, Landroid/view/ViewGroup;->addView(Landroid/view/View;I)V
    return-void

    :add_end
    invoke-virtual {v0, p1}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

    :cond_return
    return-void
.end method
"""

val moveTicketsTabPatch = bytecodePatch(
    name = "Кнопка «Тикеты» в шторке",
    description = "Переносит кнопку «Тикеты» из подменю профиля на основной уровень левой шторки — сразу под «Упоминания».",
) {
    compatibleWith("ru.fourpda.client"("1.9.44_pre7"))

    apply {
        if (classDefs[MOVE_TICKETS] == null) {
            classDefs += assemble(MOVE_TICKETS_SMALI)
        }

        val mainActivity = classDefs[MAIN_ACTIVITY] ?: error("в apk нет MainActivity")
        val setup = proxy(mainActivity).mutableClass.methods.firstOrNull { method ->
            method.name == "f" && method.parameterTypes == listOf("Landroid/os/Bundle;")
        } ?: error("не найден MainActivity.f")

        val instructions = setup.implementation!!.instructions
        val targetIndex = instructions.indices.firstOrNull { index ->
            if (index < 5) return@firstOrNull false
            val called = instructions[index].calledMethod()
            val sget = instructions[index - 5].fieldRef()
            called?.name == "setOnClickListener" &&
                sget?.definingClass == RES_IDS && sget.name == "p3"
        } ?: error("не найдена настройка nav_user_tickets")

        val call = instructions[targetIndex] as FiveRegisterInstruction
        val viewRegister = call.registerC
        if (viewRegister > 15) {
            error("кнопка тикетов попала в широкий регистр")
        }

        setup.addInstructions(
            targetIndex + 1,
            """
            invoke-static {p0, v$viewRegister}, $MOVE_TICKETS->move(Lru/fourpda/client/MainActivity;Landroid/view/View;)V
            """.trimIndent(),
        )
    }
}

package net.botwithus;

import java.util.Arrays;

import net.botwithus.vInkMaker.BotState;
import net.botwithus.rs3.imgui.ImGui;
import net.botwithus.rs3.imgui.ImGuiWindowFlag;
import net.botwithus.rs3.imgui.NativeInteger;
import net.botwithus.rs3.script.ScriptConsole;
import net.botwithus.rs3.script.ScriptGraphicsContext;

public class vInkMakerGraphicsContext extends ScriptGraphicsContext {

    private vInkMaker script;

    public vInkMakerGraphicsContext(ScriptConsole scriptConsole, vInkMaker script) {
        super(scriptConsole);
        this.script = script;
    }

    

    @Override
    public void drawSettings() {
        if (ImGui.Begin("vInkMaker", ImGuiWindowFlag.None.getValue())) {
            if (ImGui.BeginTabBar("vInkMaker", ImGuiWindowFlag.None.getValue())) {
                if (ImGui.BeginTabItem("Info", ImGuiWindowFlag.None.getValue())) {
                    
                   ImGui.Text("Current State: " + script.getBotState());
                   ImGui.Text("Current Ink: " + script.getSelectedInk());
                    ImGui.Text("");
                    ImGui.Text("");
                    ImGui.Text("Begin script with preset loaded in inventory.");
                    if (ImGui.Button("Start")) {
                        script.setBotState(BotState.MAKING_INK);
                    }
                    if (ImGui.Button("Stop")) {
                        script.setBotState(BotState.IDLE);
                    }
                    ImGui.EndTabItem();
                  
                }
                if (ImGui.BeginTabItem("Settings", ImGuiWindowFlag.None.getValue())) {
                   
                    String[] Names = {"Regular", "Greater", "Powerful"
            };
            NativeInteger selectedInk = new NativeInteger(Arrays.asList(Names).indexOf(script.getSelectedInk()));
            if (ImGui.Combo("Ink Type", selectedInk, Names)) {
                script.setSelectedInk(Names[selectedInk.get()]);
            }


                    ImGui.EndTabItem();
                }
                ImGui.EndTabBar();
            }
            ImGui.End();
        }

    }

    @Override
    public void drawOverlay() {
        super.drawOverlay();
    }
}
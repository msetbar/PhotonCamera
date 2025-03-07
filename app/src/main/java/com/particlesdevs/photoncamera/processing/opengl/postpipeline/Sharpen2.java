package com.particlesdevs.photoncamera.processing.opengl.postpipeline;

import android.hardware.camera2.CaptureResult;
import android.util.Log;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.processing.opengl.nodes.Node;
import com.particlesdevs.photoncamera.processing.parameters.IsoExpoSelector;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;

public class Sharpen2 extends Node {
    public Sharpen2() {
        super("", "Sharpening");
    }

    @Override
    public void Compile() {
    }
    float blurSize = 0.20f;
    float sharpSize = 0.9f;
    float sharpMin = 0.4f;
    float sharpMax = 1.0f;
    float denoiseActivity = 1.0f;
    @Override
    public void Run() {
        denoiseActivity = getTuning("DenoiseActivity",denoiseActivity);
        blurSize = getTuning("BlurSize", blurSize);
        sharpSize = getTuning("SharpSize", sharpSize);
        sharpMin = getTuning("SharpMin",sharpMin);
        sharpMax = getTuning("SharpMax",sharpMax);
        glProg.setDefine("INTENSE",denoiseActivity);
        glProg.setDefine("INSIZE",basePipeline.mParameters.rawSize);
        glProg.setDefine("SHARPSIZE",sharpSize);
        glProg.setDefine("SHARPMIN",sharpMin);
        glProg.setDefine("SHARPMAX",sharpMax);
        glProg.setDefine("NOISES",basePipeline.noiseS);
        glProg.setDefine("NOISEO",basePipeline.noiseO);
        glProg.useAssetProgram("lsharpening3");
        glProg.setVar("size", sharpSize);
        float sharpness = Math.max(PreferenceKeys.getSharpnessValue(), 0.0f);
        glProg.setVar("strength", sharpness);
        glProg.setTexture("InputBuffer", previousNode.WorkingTexture);
        glProg.setTexture("BlurBuffer",previousNode.WorkingTexture);
        WorkingTexture = basePipeline.getMain();
        glProg.drawBlocks(WorkingTexture);
        glProg.closed = true;
    }
}

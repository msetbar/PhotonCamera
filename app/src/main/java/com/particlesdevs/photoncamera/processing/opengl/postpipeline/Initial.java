    package com.particlesdevs.photoncamera.processing.opengl.postpipeline;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.util.Log;

import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.processing.opengl.GLFormat;
import com.particlesdevs.photoncamera.processing.opengl.GLTexture;
import com.particlesdevs.photoncamera.processing.opengl.nodes.Node;
import com.particlesdevs.photoncamera.processing.render.ColorCorrectionTransform;
import com.particlesdevs.photoncamera.processing.render.Converter;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.util.FileManager;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.Arrays;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_LINEAR;

public class Initial extends Node {
    public Initial() {
        super(0, "Initial");
    }

    @Override
    public void AfterRun() {
        lutbm.recycle();
        lut.close();
    }

    @Override
    public void Compile() {}

    GLTexture lut;
    Bitmap lutbm;
    float highersatmpy = 0.f;
    float gammaKoefficientGenerator = 2.0f;
    float gammax1 = 7.1896f;
    float gammax2 = -50.8195f;
    float gammax3 = 129.3564f;
    float tonemapx1 =-0.15f;
    float tonemapx2 = 2.55f;
    float tonemapx3 = -1.6f;
    float saturationConst = 1.f;
    float saturationGauss = 1.5f;
    float saturationRed = 0.7f;
    float eps = 0.0008f;
    @Override
    public void Run() {
        gammaKoefficientGenerator =getTuning("GammaKoefficientGenerator", gammaKoefficientGenerator);
        gammax1 =   getTuning("GammaModelX1",gammax1    );
        gammax2 =   getTuning("GammaModelX2",gammax2    );
        gammax3 =   getTuning("GammaModelX3",gammax3    );
        tonemapx1 = getTuning("TonemapModelX1",tonemapx1);
        tonemapx2 = getTuning("TonemapModelX2",tonemapx2);
        tonemapx3 = getTuning("TonemapModelX3",tonemapx3);
        saturationConst = getTuning("SaturationConst",saturationConst);
        saturationGauss = getTuning("SaturationGauss",saturationGauss);
        saturationRed =   getTuning("SaturationRed",  saturationRed);
        eps =             getTuning("Epsilon",        eps      );
        glProg.setDefine("GAMMAX1",  gammax1  );
        glProg.setDefine("GAMMAX2",  gammax2  );
        glProg.setDefine("GAMMAX3",  gammax3  );
        glProg.setDefine("TONEMAPX1",tonemapx1);
        glProg.setDefine("TONEMAPX2",tonemapx2);
        glProg.setDefine("TONEMAPX3",tonemapx3);
        glProg.setDefine("SATURATIONCONST",saturationConst);
        glProg.setDefine("SATURATIONGAUSS",saturationGauss);
        glProg.setDefine("SATURATIONRED",  saturationRed);
        glProg.setDefine("EPS",            eps      );

        float sat =(float) basePipeline.mSettings.saturation;
        if(basePipeline.mSettings.cfaPattern == 4) {
            sat = 0.f;
        }
        glProg.setDefine("SATURATION2",sat);
        glProg.setDefine("SATURATION",sat*highersatmpy);
        GLTexture TonemapCoeffs = new GLTexture(new Point(256,1),new GLFormat(GLFormat.DataType.FLOAT_16,1),FloatBuffer.wrap(basePipeline.mSettings.toneMap),GL_LINEAR,GL_CLAMP_TO_EDGE);
        /*GLTexture oldT = TonemapCoeffs;
        TonemapCoeffs = glUtils.interpolate(TonemapCoeffs,2);
        oldT.close();
        oldT = TonemapCoeffs;
        TonemapCoeffs = glUtils.interpolate(TonemapCoeffs,2);
        oldT.close();*/
        float green = ((((PostPipeline)basePipeline).analyzedBL[0]+((PostPipeline)basePipeline).analyzedBL[2]+0.0002f)/2.f)/(((PostPipeline)basePipeline).analyzedBL[1]+0.0001f);
        if(green > 0.0f && green < 1.7f) {
            float tcor = (green+1.f)/2.f;
            glProg.setDefine("TINT",tcor);
            glProg.setDefine("TINT2",((1.f/tcor+1.f)/2.f));
        }
        //float[] BL = ((PostPipeline)basePipeline).analyzedBL;
        float[] WP = basePipeline.mParameters.whitePoint;
        float minP = (WP[0]+WP[1]+WP[2])/3.f;
        glProg.setDefine("MINP",minP);
        glProg.setDefine("NEUTRALPOINT",WP);

        float[][] cube = null;
        ColorCorrectionTransform.CorrectionMode mode =  basePipeline.mParameters.CCT.correctionMode;
        if(mode == ColorCorrectionTransform.CorrectionMode.CUBES || mode == ColorCorrectionTransform.CorrectionMode.CUBE){
            glProg.setDefine("CCT", 1);
            if(basePipeline.mParameters.CCT.correctionMode == ColorCorrectionTransform.CorrectionMode.CUBES)
            cube = basePipeline.mParameters.CCT.cubes[0].Combine(basePipeline.mParameters.CCT.cubes[1],basePipeline.mParameters.whitePoint);
            else {
                cube = basePipeline.mParameters.CCT.cubes[0].cube;
            }
        }
        if(((PostPipeline)basePipeline).FusionMap != null) glProg.setDefine("FUSION", 1);
        glProg.useProgram(R.raw.initial);
        if(mode == ColorCorrectionTransform.CorrectionMode.CUBE || mode == ColorCorrectionTransform.CorrectionMode.CUBES){
            glProg.setVar("CUBE0",cube[0]);
            glProg.setVar("CUBE1",cube[1]);
            glProg.setVar("CUBE2",cube[2]);
        }
        float[] cct = basePipeline.mParameters.CCT.matrix;
        if(mode == ColorCorrectionTransform.CorrectionMode.MATRIXES){
            cct = basePipeline.mParameters.CCT.combineMatrix(basePipeline.mParameters.whitePoint);
            Log.d(Name,"CCT:"+ Arrays.toString(cct));
        }
        float[] gamma = new float[1024];
        for(int i =0; i<gamma.length;i++){
            double pos =((float)i)/gamma.length;
            gamma[i] = (float)(Math.pow(pos, 1./ gammaKoefficientGenerator));
        }
        GLTexture GammaTexture = new GLTexture(gamma.length,1,new GLFormat(GLFormat.DataType.FLOAT_16),FloatBuffer.wrap(gamma),GL_LINEAR,GL_CLAMP_TO_EDGE);
        File customlut = new File(FileManager.sPHOTON_TUNING_DIR,"initial_lut.png");
        if(customlut.exists()){
            lutbm = BitmapFactory.decodeFile(customlut.getAbsolutePath());
            glProg.setDefine("LUT",true);
        } else {
            lutbm = BitmapFactory.decodeResource(PhotonCamera.getResourcesStatic(),R.drawable.initial_lut);
        }
        lut = new GLTexture(lutbm,GL_LINEAR,GL_CLAMP_TO_EDGE,0);
        glProg.setTexture("TonemapTex",TonemapCoeffs);
        glProg.setTexture("GammaCurve",GammaTexture);
        glProg.setTexture("InputBuffer",super.previousNode.WorkingTexture);
        glProg.setTexture("LookupTable",lut);
        //glProg.setTexture("GainMap", ((PostPipeline)basePipeline).GainMap);
        //glProg.setVar("toneMapCoeffs", Converter.CUSTOM_ACR3_TONEMAP_CURVE_COEFFS);
        glProg.setVar("sensorToIntermediate",basePipeline.mParameters.sensorToProPhoto);
        glProg.setVar("intermediateToSRGB",cct);
        if(((PostPipeline)basePipeline).FusionMap != null) glProg.setTexture("FusionMap",((PostPipeline)basePipeline).FusionMap);
        Log.d(Name,"SensorPix:"+basePipeline.mParameters.sensorPix);
        glProg.setVar("activeSize",4,4,basePipeline.mParameters.sensorPix.right-basePipeline.mParameters.sensorPix.left-4,
                basePipeline.mParameters.sensorPix.bottom-basePipeline.mParameters.sensorPix.top-4);
        //glProg.setVar("neutralPoint",WP);
        Log.d(Name,"compressor:"+1.f/((float)basePipeline.mSettings.compressor));
        //glProg.setVar("saturation0",sat);
        //glProg.setVar("saturation",0.f);
        //WorkingTexture = new GLTexture(super.previousNode.WorkingTexture.mSize,new GLFormat(GLFormat.DataType.FLOAT_16, GLConst.WorkDim),null);
        WorkingTexture = basePipeline.getMain();
    }
}

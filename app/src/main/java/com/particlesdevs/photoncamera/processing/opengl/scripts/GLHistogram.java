package com.particlesdevs.photoncamera.processing.opengl.scripts;

import android.util.Log;

import com.particlesdevs.photoncamera.processing.opengl.GLBuffer;
import com.particlesdevs.photoncamera.processing.opengl.GLContext;
import com.particlesdevs.photoncamera.processing.opengl.GLFormat;
import com.particlesdevs.photoncamera.processing.opengl.GLImage;
import com.particlesdevs.photoncamera.processing.opengl.GLProg;
import com.particlesdevs.photoncamera.processing.opengl.GLTexture;


public class GLHistogram implements AutoCloseable{
    GLContext context;
    GLProg glProg;
    GLBuffer[] buffers = new GLBuffer[4];
    int histSize;
    public int[][] outputArr = new int[4][histSize];
    GLFormat histFormat = new GLFormat(GLFormat.DataType.UNSIGNED_32);
    private boolean externalContext = false;
    public boolean Rc = true;
    public boolean Gc = true;
    public boolean Bc = true;
    public boolean Ac = true;
    public boolean Custom = false;
    int resize = 3;
    public String CustomProgram = "";
    public GLHistogram() {
        this(new GLContext(1,1),256);
    }
    public GLHistogram(GLContext context) {
        this(context,256);
    }
    public GLHistogram(GLContext context, int size) {
        histSize = size;
        this.context = context;
        externalContext = true;
        glProg = context.mProgram;
        buffers[0] = new GLBuffer(histSize,histFormat);
        buffers[1] = new GLBuffer(histSize,histFormat);
        buffers[2] = new GLBuffer(histSize,histFormat);
        buffers[3] = new GLBuffer(histSize,histFormat);
    }
    public int[][] Compute(GLImage input){
        GLTexture texture = new GLTexture(input);
        int[][] out = Compute(texture);
        input.close();
        return out;
    }
    public int[][] Compute(GLTexture input){
        long time = System.currentTimeMillis();
        input.Bufferize();
        int tile = 8;
        glProg.setDefine("SCALE",resize);
        glProg.setDefine("HISTSIZE", (float)(histSize));
        glProg.setDefine("COL_R", Rc);
        glProg.setDefine("COL_G", Gc);
        glProg.setDefine("COL_B", Bc);
        glProg.setDefine("COL_A", Ac);
        glProg.setDefine("COL_CUSTOM", Custom);
        glProg.setDefine("CUSTOM_PROGRAM", CustomProgram);

        glProg.setLayout(tile,tile,1);
        glProg.useAssetProgram("histogram",true);
        glProg.setTexture("inTexture",input);
        glProg.setBufferCompute("histogramRed",buffers[0]);
        glProg.setBufferCompute("histogramGreen",buffers[1]);
        glProg.setBufferCompute("histogramBlue",buffers[2]);
        glProg.setBufferCompute("histogramAlpha",buffers[3]);
        glProg.computeManual(input.mSize.x/(resize*tile)+1,input.mSize.y/(resize*tile)+1,1);

        outputArr[0] = buffers[0].readBufferIntegers();
        outputArr[1] = buffers[1].readBufferIntegers();
        outputArr[2] = buffers[2].readBufferIntegers();
        outputArr[3] = buffers[3].readBufferIntegers();
        Log.d("GLHistogram"," elapsed:"+(System.currentTimeMillis()-time)+" ms");
        return outputArr;
    }

    @Override
    public void close() {
        if(!externalContext) {
            glProg.close();
            context.close();
        }
    }
}

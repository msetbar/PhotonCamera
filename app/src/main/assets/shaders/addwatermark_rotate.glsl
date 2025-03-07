precision highp float;
precision highp sampler2D;
uniform sampler2D InputBuffer;
uniform sampler2D Watermark;
uniform int yOffset;
uniform int rotate;
uniform ivec2 cropSize;
uniform ivec2 rawSize;
out vec4 Output;
#define WATERMARK 1
#define watersizek (15.0)
#define OFFSET 0,0
#import interpolation
void main() {
    ivec2 xy = ivec2(gl_FragCoord.xy);
    ivec2 texSize = ivec2(textureSize(InputBuffer, 0));
    vec2 texS = vec2(textureSize(InputBuffer, 0));
    vec2 watersize = vec2(textureSize(Watermark, 0));
    vec4 water;
    vec2 cr;
    xy+=ivec2(0,yOffset)+ivec2(OFFSET);
    switch(rotate){
        case 0:
        xy += ivec2(0,(rawSize.y-cropSize.y));
        cr = (vec2(xy+ivec2(0,-texSize.y))/(texS));
        Output = texelFetch(InputBuffer, xy, 0);
        break;
        case 1:
        xy += ivec2((rawSize.y-cropSize.y),0);
        cr = (vec2(xy+ivec2(-(rawSize.y-cropSize.y),-cropSize.x))/(texS));
        Output = texelFetch(InputBuffer, ivec2(texSize.x-xy.y,xy.x), 0);
        break;
        case 2:
        //xy += ivec2(0,-(texSize.y-rotatedSize.y)/4);
        cr = (vec2(xy+ivec2(0,-cropSize.y))/(texS));
        Output = texelFetch(InputBuffer, ivec2(texSize.x-xy.x,texSize.y-xy.y), 0);
        break;
        case 3:
        //xy += ivec2(-(texSize.x-rotatedSize.x)/4,0);
        cr = (vec2(xy+ivec2(0,-texSize.x))/(texS));
        Output = texelFetch(InputBuffer, ivec2(xy.y,texSize.y-xy.x),0);
        break;
    }
    #if WATERMARK == 1
    cr+=vec2(0.0,1.0/watersizek);
    cr.x*=(texS.x)/(texS.y);
    cr.x/=watersize.x/watersize.y;
    cr.x*=1.025;
    cr*=watersizek;
    if(cr.x >= 0.0 && cr.y >= 0.0){
    water = texture(Watermark,cr);
    Output = mix(Output,water,water.a);
    }
    #endif
    Output*=1.005;
    Output.a = 1.0;

}

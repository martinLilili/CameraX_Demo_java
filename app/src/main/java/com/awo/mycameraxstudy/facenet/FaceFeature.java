package com.awo.mycameraxstudy.facenet;

public class FaceFeature {
    public static final int DIMS=512;
    public float fea[];
    public float arcfacefea[];
    public FaceFeature(){
        fea=new float[DIMS];
    }
    public float[] getFeature(){
        return fea;
    }
    //比较当前特征和另一个特征之间的相似度
    public double compare(FaceFeature ff){
        double dist=0;
        for (int i=0;i<DIMS;i++)
            dist+=(fea[i]-ff.fea[i])*(fea[i]-ff.fea[i]);
        dist=Math.sqrt(dist);
        return dist;
    }


    public double compareArc(FaceFeature ff){
        double sim = 0.0;
        for (int i = 0; i < arcfacefea.length; i++)
            sim += arcfacefea[i] * ff.arcfacefea[i];
        return sim;
    }
}

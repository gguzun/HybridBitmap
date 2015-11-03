package hybridewah;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import bsi.BsiAttributeEWAH;

public class run {
	

	 public static void main(String[] args) throws java.io.IOException {
		 
		 
		// EWAHCompressedBitmap ewahBitmap1 =
		// EWAHCompressedBitmap.bitmapOf(0,2,64,1<<30);
		// EWAHCompressedBitmap ewahBitmap2
		// EWAHCompressedBitmap.bitmapOf(1,3,64,1<<30);
		// System.out.println("bitmap 1: "+ewahBitmap1);
		// System.out.println("bitmap 2: "+ewahBitmap2);
		//
		// // and
		// double startTime= System.nanoTime();
		// EWAHCompressedBitmap andbitmap = ewahBitmap1.and(ewahBitmap2);
		// double endTime= System.nanoTime();
		// System.out.println("bitmap 1 AND bitmap 2: "+andbitmap);
		// System.out.println("memory usage: " + andbitmap.sizeInBytes() +
		// " bytes");
		// System.out.println("Time to query: "+((endTime-startTime)*0.000001)+" ms");

//		EWAHCompressedBitmap ewahBitmap1 = new EWAHCompressedBitmap();
//		EWAHCompressedBitmap ewahBitmap2 = new EWAHCompressedBitmap(true);
//		EWAHCompressedBitmap ewahBitmap3 = new EWAHCompressedBitmap();
//		EWAHCompressedBitmap ewahBitmap4 = new EWAHCompressedBitmap(true);
		//int count1=0;
		//int count2=0;
for(int davai=0; davai<5;davai++){
		Random ran = new Random();
		double avvTime=0;
		double accTime=0;
		double ahvTime=0;
		double ahcTime=0;
		double ahTime=0;
	
		
		long vvSize=0;
		long ccSize=0;
		long hvSize=0;
		long hcSize=0;
		long hSize=0;
		
		HybridBitmap VVV = new HybridBitmap();
		HybridBitmap CCC = new HybridBitmap();
		HybridBitmap HHV = new HybridBitmap();
		HybridBitmap HHC = new HybridBitmap();
		HybridBitmap HH = new HybridBitmap();
int t = 0;
for(int dim=0; dim<14;dim++){

		int density[]=new int[20];	
		for(int a=0;a<2;a++){
			density[a]=20;
		}
		
		int random[]=new int[density.length];
		
		//long parsed[] = new long[density.length];
		HybridBitmap ewahBitmap[] = new HybridBitmap[density.length];
		HybridBitmap ewahBitmapV[] = new HybridBitmap[density.length];
				for(int i=0;i<density.length; i++){
			ewahBitmap[i]=new HybridBitmap();  //compressed bitmap initialization
			ewahBitmapV[i]=new HybridBitmap(true);  //verbatim bitmap initialization
	}
		
		
//		
//
//		for (int i = 0; i < 1600000; i++) {
//			long bitSTR[]=new long[density.length];
//			
//
//			
//			
//			for (int j = 0; j < 64; j++) {
//				random = generateRandomBinaryArray(density);
//				while (IntStream.of(random).sum() >1){ // if you want bitmaps that never have same setbits
//					random = generateRandomBinaryArray(density);}
//				for(int k=0; k<bitSTR.length;k++){					
//					bitSTR[k]=bitSTR[k]|(random[k]<<j);
//				}
//			}
//			
//			for(int k=0; k<bitSTR.length;k++){
//				ewahBitmap[k].add(bitSTR[k]);
//				ewahBitmapV[k].addVerbatim(bitSTR[k]);
//			}
//			
//		}
//		
//		String fileOut = "/user/g/gguzun/opt/data/temp/pointq-range";
//		
//		FileOutputStream out = new FileOutputStream(fileOut+dim);
//		ObjectOutputStream oout = new ObjectOutputStream(out);
//		oout.writeObject(ewahBitmap);
//		oout.close();
//		
//		FileOutputStream outt = new FileOutputStream(fileOut+"V"+dim);
//		ObjectOutputStream ooutt = new ObjectOutputStream(outt);
//		ooutt.writeObject(ewahBitmapV);
//		ooutt.close();
		
		
		String fileOut = "/user/g/gguzun/opt/data/temp/pointq-range";
		FileInputStream fin = new FileInputStream(fileOut+dim);
		ObjectInputStream ois = new ObjectInputStream(fin);
		HybridBitmap[] ewahBitmapTemp =new HybridBitmap[15];
		try {
			 ewahBitmapTemp = (HybridBitmap[]) ois.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(int g=0;g<density.length; g++){
			ewahBitmap[g]=ewahBitmapTemp[g];
		}
		ois.close();
		
		FileInputStream finn = new FileInputStream(fileOut+"V"+dim);
		ObjectInputStream oiss = new ObjectInputStream(finn);
		HybridBitmap[] ewahBitmapVTemp =new HybridBitmap[15];
		
		try {
			ewahBitmapVTemp = (HybridBitmap[]) oiss.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int g=0;g<density.length; g++){
			ewahBitmapV[g]=ewahBitmapVTemp[g];
		}
		oiss.close();
		
		

		for(int k=0; k<density.length;k++){
			ewahBitmap[k].density=ewahBitmap[k].cardinality()/(double) ewahBitmap[k].sizeInBits();
			ewahBitmapV[k].verbatim=true;
			ewahBitmapV[k].density=ewahBitmapV[k].cardinality()/(double) ewahBitmapV[k].sizeInBits();
			//System.out.println("Cardinality"+k+": "+1D/((double)ewahBitmap[k].cardinality()/(1600000*64))+"  | "+1D/((double)ewahBitmapV[k].cardinality()/(1600000*64)));
			
		}
		
		
		
//		 avvTime=0;
//		 accTime=0;
//		 ahvTime=0;
//		 ahcTime=0;
//		 ahTime=0;
		// int j=0;
	
		
		//EWAHCompressedBitmap HC = new EWAHCompressedBitmap();
		
		//int iterations=1;
		
		HybridBitmap VV = new HybridBitmap();
		HybridBitmap CC = new HybridBitmap();
		HybridBitmap HV = new HybridBitmap();
		HybridBitmap HC = new HybridBitmap();
		HybridBitmap H = new HybridBitmap();
		
//		double vvTime[]=new double[iterations];
//		double ccTime[]=new double[iterations];
//		double hvTime[]=new double[iterations];
//		double hcTime[]=new double[iterations];
//		double hTime[]=new double[iterations];
		 
		
	    double tc = 0.7;
		

//		 for(int j=0; j<iterations; j++ ){
				long time = 0;
				long startTime = System.nanoTime();
//				H=(ewahBitmap[0].actualsizeinwords<tc*ewahBitmapV[0].actualsizeinwords?ewahBitmap[0]:ewahBitmapV[0]).or(ewahBitmap[1].actualsizeinwords<tc*ewahBitmapV[1].actualsizeinwords?ewahBitmap[1]:ewahBitmapV[1]);
//				hSize=hSize+(ewahBitmap[0].actualsizeinwords<tc*ewahBitmapV[0].actualsizeinwords?ewahBitmap[0]:ewahBitmapV[0]).actualsizeinwords+(ewahBitmap[1].actualsizeinwords<tc*ewahBitmapV[1].actualsizeinwords?ewahBitmap[1]:ewahBitmapV[1]).actualsizeinwords+H.actualsizeinwords;
//				for(int i=0; i<density.length/4; i++){
//					H=H.or(ewahBitmap[i].actualsizeinwords<tc*ewahBitmapV[i].actualsizeinwords?ewahBitmap[i]:ewahBitmapV[i]);		
//					hSize=hSize+(ewahBitmap[i].actualsizeinwords<tc*ewahBitmapV[i].actualsizeinwords?ewahBitmap[i]:ewahBitmapV[i]).actualsizeinwords+H.actualsizeinwords;
//				}
//
////			}
//		 if(dim==0)
//				HH=H;
//			else
//				HH=H.and(HH);
//		
//		 hSize=hSize+HH.actualsizeinwords;
//			time = System.nanoTime() - startTime;
//			ahTime+=  time / (double)1000000;	
			
//			
//		
//		for(int j=0; j<iterations; j++ ){
//			long time = 0;
//			long startTime = System.nanoTime();
//			VV=ewahBitmapV[0].orV(ewahBitmapV[1]);
//			vvSize=vvSize+ewahBitmapV[0].actualsizeinwords+ewahBitmapV[1].actualsizeinwords+VV.actualsizeinwords;
//			for(int i=0; i<density.length/4; i++){
//				VV=VV.orV(ewahBitmapV[i]);	
//				vvSize=vvSize+ewahBitmapV[i].actualsizeinwords+VV.actualsizeinwords;
//			}
//			
////		}		
//		if(dim==0)
//			VVV=VV;
//		else
//			VVV=VV.andV(VVV);
//		vvSize=vvSize+VVV.actualsizeinwords;
//		time = System.nanoTime() - startTime;
//		avvTime +=  time / (double)1000000;	
//		
//		
//		
////		for(int j=0; j<iterations; j++ ){
//			 time = 0;
//			 startTime = System.nanoTime();
			CC=ewahBitmap[0].orC(ewahBitmap[1]);
			ccSize=ccSize+ewahBitmap[0].actualsizeinwords+ewahBitmap[1].actualsizeinwords+CC.actualsizeinwords;
			for(int i=0; i<density.length/4; i++){
				CC=CC.orC(ewahBitmap[i]);
				ccSize=ccSize+ewahBitmap[i].actualsizeinwords+CC.actualsizeinwords;
			}
			
//		}
		if(dim==0)
			CCC=CC;
		else
			CCC=CC.andC(CCC);
		ccSize=ccSize+CCC.actualsizeinwords;
		time = System.nanoTime() - startTime;
		accTime +=  time / (double)1000000;	
//		
//
//		
//		
////		for(int j=0; j<iterations; j++ ){
//			 time = 0;
//			 startTime = System.nanoTime();
//			HC=ewahBitmap[0].or(ewahBitmap[1]);
//			hcSize=hcSize+ewahBitmap[0].actualsizeinwords+ewahBitmap[1].actualsizeinwords+HC.actualsizeinwords;
//			for(int i=0; i<density.length/4; i++){
//				HC=HC.or(ewahBitmap[i]);		
//				hcSize=hcSize+ewahBitmap[i].actualsizeinwords+HC.actualsizeinwords;
//			}
//
////		}
//		if(dim==0)
//			HHC=HC;
//		else
//			HHC=HC.and(HHC);
//		hcSize=hcSize+HHC.actualsizeinwords;
//		time = System.nanoTime() - startTime;
//		ahcTime+=  time / (double)1000000;	
//		
////		for(int j=0; j<iterations; j++ ){
//			 time = 0;
//			 startTime = System.nanoTime();
//			HV=ewahBitmapV[0].or(ewahBitmapV[1]);
//			hvSize=hvSize+ewahBitmapV[0].actualsizeinwords+ewahBitmapV[1].actualsizeinwords+HV.actualsizeinwords;
//			for(int i=0; i<density.length/4; i++){
//				HV=HV.or(ewahBitmapV[i]);			
//				hvSize=hvSize+ewahBitmapV[i].actualsizeinwords+HV.actualsizeinwords;
//			}
//	
////		}
//		if(dim==0)
//			HHV=HV;
//		else
//			HHV=HV.and(HHV);
//		hvSize=hvSize+HHV.actualsizeinwords;
//		time = System.nanoTime() - startTime;
//		ahvTime+=  time / (double)1000000;
		
//		Arrays.sort(vvTime);
//		Arrays.sort(ccTime);
//		Arrays.sort(hvTime);
//		Arrays.sort(hcTime);
//		Arrays.sort(hTime);
//		
//		
//		
//		
//		
//		
//		for(int k=3; k<(iterations-3); k++){
//			if(avvTime==0)
//				avvTime=vvTime[k];
//			else
//			avvTime= (vvTime[k]+(9*t+k-3)*avvTime)/(9*t+k-2);
//			
//			if(accTime==0)
//				accTime=ccTime[k];
//			else
//			accTime= (ccTime[k]+(9*t+k-3)*accTime)/(9*t+k-2);
//			
//			if(ahvTime==0)
//				ahvTime=hvTime[k];
//			else
//			ahvTime= (hvTime[k]+(9*t+k-3)*ahvTime)/(9*t+k-2);
//			
//			if(ahcTime==0)
//				ahcTime=hcTime[k];
//			else
//			ahcTime= (hcTime[k]+(9*t+k-3)*ahcTime)/(9*t+k-2);
//			
//			if(ahTime==0)
//				ahTime=hTime[k];
//			else
//			ahTime= (hTime[k]+(9*t+k-3)*ahTime)/(9*t+k-2);
//		}
		
		//System.out.println(VVV.cardinality()+"\t"+CCC.cardinality()+"\t"+HHV.cardinality()+"\t"+HHC.cardinality()+"\t"+HH.cardinality());
		//System.out.println("done dimension "+dim);
}
		

		System.out.println("point queries: ");
		System.out.println(avvTime+"\t"+accTime+"\t"+ahvTime+"\t"+ahcTime+"\t"+ahTime);
		System.out.println(8*vvSize+"\t"+8*ccSize+"\t"+8*hvSize+"\t"+8*hcSize+"\t"+8*hSize);
}		
		
//		int iterations =15;
//		double verbatim[]=new double[iterations];
//		double verbatimCompress[]=new double[iterations];
//		double compressed[]=new double[iterations];
//		double compressedDecompress[]=new double[iterations];
//		double hybrid[]=new double[iterations];
//		double hybridCompress[]=new double[iterations];
//		double optHybrid[]=new double[iterations];
//		double optCompress[]=new double[iterations];
//		double optVerbatim[]=new double[iterations];

//		for(int j=0;j<iterations;j++){
//		EWAHCompressedBitmap resultCompressed = new EWAHCompressedBitmap();
//		EWAHCompressedBitmap resultCompressedVerb = new EWAHCompressedBitmap();
//		EWAHCompressedBitmap resultVerbatim = new EWAHCompressedBitmap(true);		
//		EWAHCompressedBitmap resultVerbatimCom = new EWAHCompressedBitmap();
//		EWAHCompressedBitmap resultHybridCom = new EWAHCompressedBitmap();
//		EWAHCompressedBitmap resultHybrid = new EWAHCompressedBitmap();
//		EWAHCompressedBitmap optimumVerbatim = new EWAHCompressedBitmap();
//		EWAHCompressedBitmap optimumCompress = new EWAHCompressedBitmap();
//		EWAHCompressedBitmap optimumHybrid = new EWAHCompressedBitmap();
//		
//	
//		
//		
//		
//
//		long time = 0;
//		long startTime = System.nanoTime();
//		ewahBitmapV[0].orVerbatim(ewahBitmapV[1], resultVerbatim);
//		time = System.nanoTime() - startTime;
//		verbatim[j] = (double) time / 1000000;
//		System.out.println("TIme Verbatim:  " + verbatim[j]);
//		
//		 time = 0;
//		  startTime = System.nanoTime();
//		ewahBitmapV[0].orVerbatimCompress(ewahBitmapV[1], resultVerbatimCom);
//		time = System.nanoTime() - startTime;
//		verbatimCompress[j] = (double) time / 1000000;
//		System.out.println("TIme Verbatim compress:  " + verbatimCompress[j]);
//
//		time = 0;
//		startTime = System.nanoTime();
//		ewahBitmap1.orToContainer(ewahBitmap3, resultCompressed);
//		time = System.nanoTime() - startTime;
//		compressed[j] = (double) time / 1000000;
//		System.out.println("TIme Compressed:  " + compressed[j]);
//		//System.out.println(resultCompressed.getPositions());
//		
//		time = 0;
//		startTime = System.nanoTime();
//		ewahBitmap1.orToContainerDecompress(ewahBitmap3, resultCompressedVerb);
//		time = System.nanoTime() - startTime;
//		compressedDecompress[j] = (double) time / 1000000;
//		System.out.println("TIme Compressed Verb:  " + compressedDecompress[j]);
//		
//		time = 0;
//		startTime = System.nanoTime();
//		ewahBitmap1.orHybrid(ewahBitmap4, resultHybrid);
//		time = System.nanoTime() - startTime;
//		hybrid[j] = (double) time / 1000000;
//		System.out.println("TIme Hybrid:  " + hybrid[j]);
//
//		time = 0;
//		startTime = System.nanoTime();
//		ewahBitmap1.orHybridCompress(ewahBitmap4, resultHybridCom);
//		time = System.nanoTime() - startTime;
//		hybridCompress[j] = (double) time / 1000000;
//		System.out.println("TIme Hybrid Compress:  " + hybridCompress[j]);
//		
//		 time = 0;
//		startTime = System.nanoTime();
//		optimumHybrid = ewahBitmap1.or(ewahBitmap4);
//		time = System.nanoTime() - startTime;
//		optHybrid[j] = (double) time / 1000000;
//		System.out.println("TIme Optimum Hybrid :  " + optHybrid[j]);
//		
//		time = 0;
//		startTime = System.nanoTime();
//		optimumCompress = ewahBitmap1.or(ewahBitmap3);
//		time = System.nanoTime() - startTime;
//		optCompress[j] = (double) time / 1000000;
//		System.out.println("TIme Optimum Compress :  " + optCompress[j]);
//		
//		time = 0;
//		startTime = System.nanoTime();
//		optimumVerbatim= ewahBitmap2.or(ewahBitmap4);
//		time = System.nanoTime() - startTime;
//		optVerbatim[j] = (double) time / 1000000;
//		System.out.println("TIme Optimum Verbatim :  " + optVerbatim[j]);
//		
//		
//	//	System.out.println(ewahBitmap1.cardinality());
//	//	System.out.println(ewahBitmap2.cardinality());
//	//	System.out.println(ewahBitmap1.getPositions());
//	//	System.out.println(ewahBitmap2.getPositions());
//		
//		
//		
//		
//	//	System.out.println(optimumHybrid.cardinality() + "    " + optimumCompress.cardinality()+ "    " + optimumVerbatim.cardinality());
//
//		System.out.println(resultCompressed.cardinality() + "    " + resultCompressedVerb.cardinality()+ "    " + resultVerbatim.cardinality()+ "    " + resultVerbatimCom.cardinality()+ "    " + resultHybridCom.cardinality()+ "    " + resultHybrid.cardinality());
//		System.out.println(verbatim[j]+","+verbatimCompress[j]+","+compressed[j]+","+compressedDecompress[j]+","+hybrid[j]+","+hybridCompress[j]+","+optVerbatim[j]+","+optCompress[j]+","+optHybrid[j]);
//		}
//		Arrays.sort(verbatim);
//		Arrays.sort(verbatimCompress);
//		Arrays.sort(compressed);
//		Arrays.sort(compressedDecompress);
//		Arrays.sort(hybrid);
//		Arrays.sort(hybridCompress);
//		Arrays.sort(optVerbatim);
//		Arrays.sort(optCompress);
//		Arrays.sort(optHybrid);
//		
//		double averbatim=0;
//		double averbatimCompress=0;
//		double acompressed=0;
//		double acompressedDecompress=0;
//		double ahybrid=0;
//		double ahybridCompress=0;
//		double aoptVerbatim=0;
//		double aoptCompress=0;
//		double aoptHybrid=0;
//		
//		
//		
//		for(int k=3; k<(iterations-3); k++){
//			averbatim= (verbatim[k]+(k-3)*averbatim)/(k-2);
//			averbatimCompress= (verbatimCompress[k]+(k-3)*averbatimCompress)/(k-2);
//			acompressed= (compressed[k]+(k-3)*acompressed)/(k-2);
//			acompressedDecompress= (compressedDecompress[k]+(k-3)*acompressedDecompress)/(k-2);
//			ahybrid= (hybrid[k]+(k-3)*ahybrid)/(k-2);
//			ahybridCompress= (hybridCompress[k]+(k-3)*ahybridCompress)/(k-2);
//			aoptVerbatim= (optVerbatim[k]+(k-3)*aoptVerbatim)/(k-2);
//			aoptCompress= (optCompress[k]+(k-3)*aoptCompress)/(k-2);
//			aoptHybrid= (optHybrid[k]+(k-3)*aoptHybrid)/(k-2);
//		}
//		
//		
//		
//		
//		System.out.println(averbatim+","+averbatimCompress+","+acompressed+","+acompressedDecompress+","+ahybrid+","+ahybridCompress+","+aoptVerbatim+","+aoptCompress+","+aoptHybrid);
//		
//	//	System.out.println(resultCompressed.getPositions());
//	//	System.out.println(resultVerbatim.getPositions());
//		
//		EWAHCompressedBitmap test1 = new EWAHCompressedBitmap(true);
//		EWAHCompressedBitmap test2 = new EWAHCompressedBitmap(true);
//		
//		test1.addVerbatim(Long.parseUnsignedLong("0000000000000000000000000000000000000000000000000000000000000000",2));
//		test1.addVerbatim(Long.parseUnsignedLong("0000000000000000000000000000000000000000000000000000000000000000",2));
//		test1.addVerbatim(Long.parseUnsignedLong("1000000000000000000000000000000000000000000000000000000000000000",2));
//		
//		test2.addVerbatim(Long.parseUnsignedLong("0000000000000000000000000000000000000000000000000000000000000000",2));
//		test2.addVerbatim(Long.parseUnsignedLong("0000000000000000000000000000000000000000000000000000000000000000",2));
//		test2.addVerbatim(Long.parseUnsignedLong("1111111111111111111111111111111111111111111111111111111111111111",2));
//		
//		
//		
//		
//		EWAHCompressedBitmap test3 = new EWAHCompressedBitmap();
//		EWAHCompressedBitmap test4 = new EWAHCompressedBitmap();
//		test1.andToContainer(test2, test3);
//		test1.andToContainer(test2, test4);
//		
		Runtime runtime = Runtime.getRuntime();
		runtime.gc();
		long memory=runtime.totalMemory()-runtime.freeMemory();
		System.out.println("Used memory in bytes: "+memory);
		
		
	//	System.out.println(test1.getPositions());
	//	System.out.println(test2.getPositions());

	 }
	 
	 

		static  int[] generateRandomBinaryArray(int[] density){
			Random rand = new Random();
			int random[]= new int[density.length];
			for(int j=0; j<density.length; j++){
				if (rand.nextInt(density[j]) == 0)
					random[j] = 1;
				else
					random[j] = 0;
			}
			return random;
		}
}

import java.util.concurrent.Semaphore;

import lib.*;
import popjava.annotation.POPAsyncConc;
import popjava.annotation.POPAsyncSeq;
import popjava.annotation.POPClass;
import popjava.annotation.POPConfig;
import popjava.annotation.POPConfig.Type;
import popjava.annotation.POPSyncSeq;

@POPClass
public class MatrixWorker {

	private Matrix2Dlc c;
	private int nbLinesA, nbColsA, nbColsB;
	private int id;
	private Matrix2Dlc aMat;
	private Matrix2Dcl bMat;
	private int NbCores = 1;

	private Timer timer = new Timer();
	private double computeTime, waitTime;

	private Semaphore workerSemaphore = new Semaphore(0);

	public MatrixWorker() {
	    
	}
	
	public MatrixWorker(int i, int nbLineA, int nbColA, int nbColB,
			@POPConfig(Type.URL) String machine, int NbC) {
		nbLinesA = nbLineA;
		nbColsA = nbColA;
		nbColsB = nbColB;
		c = new Matrix2Dlc(nbLineA, nbColB);
		timer.start();
		NbCores = NbC;
	}

	public MatrixWorker(int i, int nbLineA, int nbColA, int nbColB,
			@POPConfig(Type.URL) String machine) {
		this(i, nbLineA, nbColA, nbColB, machine, 4);
	}

	@POPAsyncSeq
	public void solve(Matrix2Dlc a, Matrix2Dcl b) {
		waitTime = timer.elapsed();
		aMat = a;
		bMat = b;
		
		// Launch NbCores "SolveCore" methods (Threads)
		for (int i = 0; i < NbCores; i++) {
			this.solveCore(i);
		}

		// Wait until all "SolveCore" methods are terminated
		try{
			workerSemaphore.acquire(NbCores);
		}catch(InterruptedException e){
			e.printStackTrace();
		}
		

		computeTime = timer.elapsed() - waitTime;
		timer.stop();
	}

	@POPAsyncConc
	public void solveCore(int st) {
		for (int j = st; j < nbLinesA; j = j + NbCores) {
			for (int k = 0; k < nbColsB; k++) {
				for (int l = 0; l < nbColsA; l++) {
					//System.out.println(j+" "+ k+" "+c.get(j, k)+" "+aMat.get(j, l) +" * "+bMat.get(l, k));
					c.set(j, k, c.get(j, k) + aMat.get(j, l) * bMat.get(l, k));
				}
			}
		}

		workerSemaphore.release();
	}

	@POPSyncSeq
	public Matrix2Dlc getResult() {
		return c;
	}

	@POPSyncSeq
	public double getWaitTime() {
		return waitTime;
	}

	@POPSyncSeq
	public double getComputeTime() {
		return computeTime;
	}

}
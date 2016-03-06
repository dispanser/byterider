package org.kulturguerilla.byterider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.kulturguerilla.byterider.ByteRiderDemo.MyEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ByteRiderDemoTests
{
	private static final Logger logger = LoggerFactory.getLogger(ByteRiderDemoTests.class);

	static final int RUNS = 100;

	@Test public void testExerciseCombinations() {
		this.exerciseCombinations(new ByteRiderDemo());
	}

	@Test public void parallelExercising() throws InterruptedException {
		long start = System.currentTimeMillis();
		for(int i=1; i<5; ++i) {
			int threads = 1 << i;
			logger.debug( "starting w/ #" + threads + " threads @ " + (System.currentTimeMillis() - start));
			testParallel(threads);
			logger.debug( "finished w/ #" + threads + " threads @ " + (System.currentTimeMillis() - start));
		}
	}


	public void testParallel(int threads) throws InterruptedException {
		ExecutorService es = Executors.newFixedThreadPool(threads);
		for(int i=0; i<threads; ++i) {
			es.execute(new ByteRiderExerciser());
		}
		es.shutdown();
		es.awaitTermination(100, TimeUnit.SECONDS);
	}

	public class ByteRiderExerciser implements Runnable {
		public void run() {
			ByteRiderDemo brd = new ByteRiderDemo();
			for(int i=0; i<RUNS; ++i) {
				exerciseCombinations(brd);
			}
		}
	}


	public void exerciseCombinations(ByteRiderDemo brd) {
		List<Boolean> boolValues = Arrays.asList(true, false);
		List<Integer> xRange = Arrays.asList(-32767, 0, 32768);
		List<Integer> yRange = Arrays.asList(-16383, 0, 16384);
		List<MyEnum> ees = Arrays.asList(MyEnum.NONE, MyEnum.V1, MyEnum.V2);
		for(Boolean drv : boolValues) {
			for(Boolean frw: boolValues) {
				for(int n2c = 0; n2c <= 6; ++n2c) {
					for(int x : xRange) {
						for(int y : yRange) {
							for(MyEnum e : ees) {
								brd.setDrivable(drv);
								brd.setNet2Class(n2c);
								brd.setFreeway(frw);
								brd.setCoords(x, y);
								brd.setEnum(e);
								String as = String.format("d %s, f %s, n2c %d, x %d y %d, data: %s",
										drv, frw, n2c, x, y, Long.toBinaryString(brd.data));
								assertThat(brd.isDrivable()).as(as).isEqualTo(drv);
								assertThat(brd.getNet2Class()).as(as).isEqualTo(n2c);
								assertThat(brd.isFreeway()).as(as).isEqualTo(frw);
								assertThat(brd.getX()).as(as).isEqualTo(x);
								assertThat(brd.getY()).as(as).isEqualTo(y);
								assertThat(brd.getEnum()).as(as).isEqualTo(e);
							}
						}
					}
				}
			}
		}
	}
}

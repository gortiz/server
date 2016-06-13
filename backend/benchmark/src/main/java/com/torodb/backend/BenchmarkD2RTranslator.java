package com.torodb.backend;

import static com.torodb.backend.util.TestDataFactory.COLL1;
import static com.torodb.backend.util.TestDataFactory.DB1;
import static com.torodb.backend.util.TestDataFactory.InitialView;

import java.util.ArrayList;
import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.torodb.backend.util.InMemoryRidGenerator;
import com.torodb.backend.util.TestDataFactory;
import com.torodb.core.d2r.D2RTranslator;
import com.torodb.core.transaction.metainf.MetainfoRepository.SnapshotStage;
import com.torodb.core.transaction.metainf.MutableMetaSnapshot;
import com.torodb.kvdocument.values.KVDocument;
import com.torodb.metainfo.cache.mvcc.MvccMetainfoRepository;

public class BenchmarkD2RTranslator {

	private static InMemoryRidGenerator ridGenerator = new InMemoryRidGenerator();
	
	@State(Scope.Thread)
	public static class TranslateState {
		
		public List<KVDocument> document=new ArrayList<>();

		@Setup(Level.Invocation)
		public void setup(){
			document=new ArrayList<>();
			for (int i=0;i<100;i++){
				document.add(TestDataFactory.buildDoc());
			}
		}
	}
	

	@Benchmark
	@Fork(value=5)
	@BenchmarkMode(value=Mode.Throughput)
	@Warmup(iterations=3)
	@Measurement(iterations=10) 
	public void benchmarkTranslate(TranslateState state, Blackhole blackhole) {
		MvccMetainfoRepository mvccMetainfoRepository = new MvccMetainfoRepository(InitialView);
		MutableMetaSnapshot mutableSnapshot;
		try (SnapshotStage snapshot = mvccMetainfoRepository.startSnapshotStage()) {
			mutableSnapshot = snapshot.createMutableSnapshot();
		}
		D2RTranslator translator = new D2RTranslatorImpl(ridGenerator, mutableSnapshot, DB1, COLL1);
		for(KVDocument doc: state.document){
			translator.translate(doc);
		}
		blackhole.consume(translator.getCollectionDataAccumulator());
	}
	
}

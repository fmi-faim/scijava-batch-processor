package org.scijava.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.imagej.table.Table;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.module.ModuleService;
import org.scijava.script.ScriptInfo;
import org.scijava.service.SciJavaService;

public class BatchServiceTest {

	private Context context;

	@Before
	public void initialize() {
		context = new Context(SciJavaService.class);
	}

	@After
	public void disposeContext() {
		if (context != null) {
			context.dispose();
			context = null;
		}
	}

	@Test
	public void testContext() {
		final BatchService batchService = context
				.getService(BatchService.class);
		assertNotNull(batchService);
	}
	
	@Test
	public void testBatchableFileInputs() {
		String script = "" //
			+ "#@ File fileInput\n" //
			+ "#@ String stringInput\n" //
			+ "#@ Integer integerInput\n" //
			+ "#@ Double doubleInput\n" //
			+ "";
		ScriptInfo scriptInfo = createInfo(script);
		BatchService batchService = context.getService(BatchService.class);
		List<ModuleItem<?>> compatibleInputs = batchService.batchableInputs(
			scriptInfo);
		assertEquals("Wrong number of batchable inputs", 1, compatibleInputs
			.size());
	}

	@Test
	public void testNoCompatibleInputs() {
		String script = "" //
			+ "#@ String stringInput\n" //
			+ "#@ Integer integerInput\n" //
			+ "#@ Double doubleInput\n" //
			+ "";
		ScriptInfo scriptInfo = createInfo(script);
		BatchService batchService = context.getService(BatchService.class);
		List<ModuleItem<?>> compatibleInputs = batchService.batchableInputs(
			scriptInfo);
		assertTrue("Wrong inputs found to be compatible", compatibleInputs
			.isEmpty());
	}

	@Test
	public void testModuleBatchProcessor() {
		String script = "" //
				+ "#@ File input\n" //
				+ "#@output result\n" //
				+ "" //
				+ "result = input";
		ScriptInfo scriptInfo = createInfo(script);

		assertEquals("Wrong script language", "Groovy", scriptInfo
				.getLanguage().getLanguageName());

		File[] files = new File[3];
		files[0] = new File("foo.txt");
		files[1] = new File("bar.txt");
		files[2] = new File("quo.txt");

		HashMap<String, Object> inputMap = new HashMap<>();
		inputMap.put("moduleInfo", scriptInfo);
		inputMap.put("inputChoice", "input");
		inputMap.put("inputFileList", files);
		//inputMap.put("outputFolder", null);
		ModuleService moduleService = context.getService(ModuleService.class);
		CommandService commandService = context
				.getService(CommandService.class);
		CommandInfo commandInfo = commandService
				.getCommand(ModuleBatchProcessor.class);
		Module module = moduleService.createModule(commandInfo);
		try {
			module = moduleService.run(module, true, inputMap).get();
		} catch (InterruptedException | ExecutionException exc) {
			// TODO Auto-generated catch block
			exc.printStackTrace();
		}
		Table<?, ?> outputs = (Table<?, ?>) module.getOutput("outputTable");

		assertEquals("Wrong number of output columns", 1,
				outputs.getColumnCount());
		assertEquals("Wrong number of output rows", 3, outputs.getRowCount());
		assertEquals("Wrong column header", "result",
				outputs.getColumnHeader(0));
		assertEquals("Wrong file name", "quo.txt", outputs.getRowHeader(2));
	}

	/* --- Helper methods --- */

	private ScriptInfo createInfo(String script) {
		StringReader scriptReader = new StringReader(script);
		return new ScriptInfo(context, "Foo.groovy", scriptReader);
	}
}

package net.smackem.zlang.pad;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.WindowEvent;
import net.smackem.zlang.compiler.ZLCompiler;
import net.smackem.zlang.emit.bytecode.ByteCodeWriterOptions;
import net.smackem.zlang.interpret.Interpreter;
import net.smackem.zlang.modules.SourceFileLocation;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;

public class PrimaryController {

    private static final KeyCombination keyCombinationRun;
    private static final String prefSource = "primary.source";
    private static final String prefDividerPos = "primary.dividerPos";
    private static final String startModuleName = "start";

    @FXML
    private SplitPane splitPane;
    @FXML
    private TextArea output;
    @FXML
    private CodeEditor editor;

    static {
        keyCombinationRun = new KeyCodeCombination(KeyCode.F5);
    }

    @FXML
    private void initialize() {
        final Preferences prefs = Preferences.userNodeForPackage(PrimaryController.class);
        final String source = prefs.get(prefSource, """
                fn main() {
                    log "Hello, World!"
                }
                """);
        this.splitPane.setDividerPositions(prefs.getDouble(prefDividerPos, 0.6));
        this.editor.replaceText(source);
        Platform.runLater(() -> {
            this.output.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, this::onWindowCloseRequest);
        });
    }

    private <T extends Event> void onWindowCloseRequest(T t) {
        persistPreferences();
    }

    private void persistPreferences() {
        final Preferences prefs = Preferences.userNodeForPackage(PrimaryController.class);
        prefs.put(prefSource, this.editor.getText());
        prefs.putDouble(prefDividerPos, this.splitPane.getDividerPositions()[0]);
    }

    @FXML
    private void runProgram() {
        final String text = this.editor.getText();
        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        persistPreferences();

        final ZLCompiler.CompilationResult result;
        try {
            result = ZLCompiler.compile(sourceRepository(text), startModuleName,
                    new ByteCodeWriterOptions().isMemoryImage(true),
                    new ByteCodeWriterOptions().isMemoryImage(false));
        } catch (Exception e) {
            this.output.setText("ERROR: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        writeZapToDisk(result.zaps().get(1));

        final Map<String, Object> globals;
        try (final AutoCloseable ignored = captureStdOut(output)) {
            globals = Interpreter.run(result.firstZap(), result.program());
            System.out.println("TEST");
        } catch (Exception e) {
            this.output.setText("ERROR: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        final String sb = printGlobals(globals, "") +
                          "--------------------------------------------" +
                          output.toString(StandardCharsets.US_ASCII);
        this.output.setText(sb);
    }

    private SourceFileLocation sourceRepository(String startModuleSource) {
        return moduleName -> {
            if (Objects.equals(startModuleName, moduleName)) {
                return new ByteArrayInputStream(startModuleSource.getBytes(StandardCharsets.UTF_8));
            }
            return openStdLibFile(moduleName);
        };
    }

    private AutoCloseable captureStdOut(ByteArrayOutputStream os) {
        final PrintStream oldOut = System.out;
        final PrintStream out = new PrintStream(os, false, StandardCharsets.US_ASCII);
        System.setOut(out);
        return () -> {
            out.flush();
            System.setOut(oldOut);
            out.close();
        };
    }

    private void writeZapToDisk(ByteBuffer zap) {
        final Path path = Paths.get(System.getProperty("user.home"), "pad-start.zap");
        try (final OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            os.write(zap.array(), zap.arrayOffset(), zap.limit());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private InputStream openStdLibFile(String moduleName) throws IOException {
        final String stdLibDir = System.getProperty("zl.stdlib.dir");
        final Path modulePath = Path.of(stdLibDir, moduleName + ".zl");
        return Files.newInputStream(modulePath, StandardOpenOption.READ);
    }

    private static String printGlobals(Map<String, Object> globals, String indent) {
        final StringBuilder sb = new StringBuilder();
        globals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(entry ->
                        sb.append(indent)
                            .append(entry.getKey())
                            .append(" = ")
                            .append(printValue(entry.getValue(), indent))
                            .append(System.lineSeparator()));
        return sb.toString();
    }

    private static String printValue(Object obj, String indent) {
        if (obj == null) {
            return "(nil)";
        }
        if (obj instanceof Map<?, ?> map) {
            //noinspection unchecked
            return System.lineSeparator() + printGlobals((Map<String, Object>) map, indent + "    ");
        }
        if (obj instanceof int[] array) {
            return Arrays.toString(array);
        }
        if (obj instanceof long[] array) {
            return Arrays.toString(array);
        }
        if (obj instanceof double[] array) {
            return Arrays.toString(array);
        }
        if (obj instanceof byte[] array) {
            return Arrays.toString(array);
        }
        return obj.toString();
    }

    public void onKeyPressed(KeyEvent keyEvent) {
        if (keyCombinationRun.match(keyEvent)) {
            runProgram();
        }
    }
}

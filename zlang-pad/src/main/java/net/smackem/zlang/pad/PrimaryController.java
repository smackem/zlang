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
import net.smackem.zlang.lang.CompilationErrorException;
import net.smackem.zlang.modules.ParsedModule;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.prefs.Preferences;

public class PrimaryController {

    private static final KeyCombination KEY_COMBINATION_RUN;
    private static final String PREF_SOURCE = "primary.source";
    private static final String PREF_DIVIDER_POS = "primary.dividerPos";

    @FXML
    private SplitPane splitPane;
    @FXML
    private TextArea output;
    @FXML
    private CodeEditor editor;

    static {
        KEY_COMBINATION_RUN = new KeyCodeCombination(KeyCode.F5);
    }

    @FXML
    private void initialize() {
        final Preferences prefs = Preferences.userNodeForPackage(PrimaryController.class);
        final String source = prefs.get(PREF_SOURCE, """
                fn main() {
                    log "Hello, World!"
                }
                """);
        this.splitPane.setDividerPositions(prefs.getDouble(PREF_DIVIDER_POS, 0.6));
        this.editor.replaceText(source);
        Platform.runLater(() -> {
            this.output.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, this::onWindowCloseRequest);
        });
    }

    private <T extends Event> void onWindowCloseRequest(T t) {
        final Preferences prefs = Preferences.userNodeForPackage(PrimaryController.class);
        prefs.put(PREF_SOURCE, this.editor.getText());
        prefs.putDouble(PREF_DIVIDER_POS, this.splitPane.getDividerPositions()[0]);
    }

    @FXML
    private void runProgram() {
        final String text = this.editor.getText();
        final String entryModuleName = "start";

        final ZLCompiler.CompilationResult result;
        try {
            result = ZLCompiler.compile(moduleName -> {
                if (Objects.equals(entryModuleName, moduleName)) {
                    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
                }
                return openStdLibFile(moduleName);
            }, entryModuleName, new ByteCodeWriterOptions().isMemoryImage(true));
        } catch (Exception e) {
            this.output.setText("ERROR: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        final Map<String, Object> globals = Interpreter.run(result.zap(), result.program());
        this.output.setText(printGlobals(globals, ""));
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
        if (KEY_COMBINATION_RUN.match(keyEvent)) {
            runProgram();
        }
    }
}

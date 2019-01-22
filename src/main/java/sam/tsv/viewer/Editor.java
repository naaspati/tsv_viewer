package sam.tsv.viewer;

import java.io.File;
import java.io.IOException;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxBindings;
import sam.fx.helpers.FxFxml;
import sam.io.serilizers.StringReader2;
import sam.io.serilizers.StringWriter2;
import sam.reference.WeakAndLazy;

public class Editor {
	private static volatile Editor INSTANCE;

	public static Editor getInstance() {
		if (INSTANCE != null)
			return INSTANCE;

		synchronized (Editor.class) {
			if (INSTANCE != null)
				return INSTANCE;

			INSTANCE = new Editor();
			return INSTANCE;
		}
	}
	
	@FXML private TextArea content;
	@FXML private Text meta;

	private final Stage stage;
	private File lastFile;
	private String result;
	

	private Editor() {
		stage = new Stage(StageStyle.UTILITY);
		
		try {
			FxFxml.load(this, stage, this);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.initOwner(App.getStage());
		stage.setTitle("Editor");
		stage.getScene().addEventFilter(KeyEvent.KEY_RELEASED, e -> {
			if(e.getCode() == KeyCode.ESCAPE)
				stage.hide();
		});
		meta.textProperty().bind(FxBindings.map(content.textProperty(), t -> "chars: ".concat(Integer.toString(t == null ? 0 : t.length()))));
	}
	
	public String get(String current) {
		result = null;
		content.setText(current);
		Window w = stage.getOwner();
		stage.setX(w.getX() + w.getWidth()/2 - stage.getWidth()/2);
		stage.setY(w.getY() + w.getHeight()/2 - stage.getHeight()/2);
		stage.show();
		return result;
	}
	
	private static final ExtensionFilter TEXT = new ExtensionFilter("text", "*.txt","*.text");
	
	private WeakAndLazy<FileChooser> filechooser = new WeakAndLazy<>(() -> {
		FileChooser fc = new FileChooser();
		fc.getExtensionFilters().add(TEXT);
		fc.getExtensionFilters().add(new ExtensionFilter("all", "*.*"));
		return fc;
	});
	
	private File chooseFile(boolean importAction) {
		FileChooser fc = filechooser.get();
		fc.setSelectedExtensionFilter(TEXT);
		
		if(lastFile != null) {
			fc.setInitialDirectory(lastFile.getParentFile());
			fc.setInitialFileName(lastFile.getName());
		}
		
		fc.setTitle(importAction ? "Import" : "Export");
		File f = importAction ? fc.showOpenDialog(App.getStage()) : fc.showSaveDialog(App.getStage());
		
		if(f == null)
			return null;
		
		if(f.getName().endsWith(".txt"))
			lastFile = f;
		return f;
	}
	
	@FXML
	private void importAction(Event e) {
		File file = chooseFile(true);
		if(file == null) 
			return;
		
		try {
			content.setText(StringReader2.getText(file.toPath()));
		} catch (IOException e1) {
			FxAlert.showErrorDialog(file, "failed to read file", e1);
		}
	}
	
	@FXML
	private void exportAction(Event e) {
		File file = chooseFile(false);
		
		if(file == null) 
			return;
		
		try {
			String s = content.getText();
			StringWriter2.setText(file.toPath(), s == null ? "" : s);
		} catch (IOException e1) {
			FxAlert.showErrorDialog(file, "failed to write file", e1);
		}
	}
	@FXML
	private void clearAction(Event e) {
		content.clear();
	}
	@FXML
	private void applyAction(Event e) {
		this.result = content.getText();
		stage.hide();
	}
}

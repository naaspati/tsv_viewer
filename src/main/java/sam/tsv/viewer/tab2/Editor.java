package sam.tsv.viewer.tab2;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import sam.fx.helpers.FxHBox;
import sam.myutils.MyUtilsCheck;
import sam.tsv.viewer.tab2.TableCol.TableCell2;

public class Editor extends BorderPane {
	private final TextArea content = new TextArea();
	private final Button ok = new Button("OK");
	private final Button cancel = new Button("CANCEL");
	private TableCell2 cell;
	
	public Editor() {
		setId("editor");
		setCenter(content);
		CheckBox cb = new CheckBox("Wrap Text");
		cb.selectedProperty().bindBidirectional(content.wrapTextProperty());
		setBottom(FxHBox.buttonBox(cb, FxHBox.maxPane(), ok, cancel));
		
		content.setOnKeyReleased(e -> {
			if(e.getCode() == KeyCode.ESCAPE){
				cancel.fire();
				e.consume();
			}
		});
		cancel.setOnAction(this::cancelAction);
		ok.setOnAction(this::okAction);
	}
	private void okAction(ActionEvent e) {
		cell.commitEdit(content.getText());
		e.consume();
	}
	private void cancelAction(ActionEvent e) {
		cell.cancelEdit();
		e.consume();
	}
	void close() {
		content.setText(null);
		onClose.run();
	}
	void start(TableCell2 cell) {
		this.cell = cell;
		String s = cell.getItem();
		content.setText(MyUtilsCheck.isEmpty(s) ? "" : s);
		onstart.run();
		Platform.runLater(() -> content.requestFocus());
	}
	private Runnable onstart;
	public void onstart(Runnable r) {
		this.onstart = r;
	}
	private Runnable onClose;
	public void onClose(Runnable r) {
		this.onClose = r;
	}
}

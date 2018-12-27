package sam.tsv.viewer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import sam.config.Session;
import sam.fx.alert.FxAlert;
import sam.fx.helpers.FxBindings;
import sam.fx.popup.FxPopupShop;
import sam.tsv.viewer.tab2.Tab2;

public class App extends Application {
	public static void main(String[] args) {
		launch(args);
	}

	public static Stage getStage() {
		return stage;
	}
	public static App getInstance() {
		return instance;
	}
	
	private final Session session = Session.global(); 
	
	private static App instance;
	private static Stage stage;
	private final TabPane tabPane = new TabPane();
	private final BorderPane root = new BorderPane(tabPane);
	private final ReadOnlyObjectProperty<Tab> currentTab = tabPane.getSelectionModel().selectedItemProperty();
	private final ObjectBinding<Path> currentFile = FxBindings.map(currentTab, t -> t == null ? null : ((Tab2)t).getPath());
	private final Text meta = new Text();

	public boolean loadFromClipboard() {
		Clipboard cb = Clipboard.getSystemClipboard();

		if(!cb.hasString()){
			FxPopupShop.showHidePopup("Clipboard don't have text", 1500);
			return false;
		}

		String[][] data = Stream.of(cb.getString().split("\n")).map(s -> s.split("\t")).toArray(String[][]::new);

		if(data.length == 0){
			FxPopupShop.showHidePopup("Clipboard dont have data", 1500);
			return false;
		}

		int max = 0;
		for (String[] s : data) {
			if(s.length > max)
				max = s.length;
		}
		if(max == 0){
			FxPopupShop.showHidePopup("Clipboard dont have data", 1500);
			return false;
		}
		/* TODO
		 * clear();
		tsv = Tsv.builder().columnNames(IntStream.range(0, max).mapToObj(String::valueOf).toArray(String[]::new)).collection(getItems()).build();
		for (String[] s : data) tsv.addRow(s);
		workingFile.set(null);
		fillColumns();
		return true;
		 */

		return false;
	}

	@Override
	public void start(Stage stage) {
		App.stage = stage;
		FxPopupShop.setParent(stage);
		FxAlert.setParent(stage);

		tabPane.setSide(Side.BOTTOM);
		App.instance = this;

		root.setTop(new BorderPane(new Buttons(currentTab), new MenuBar2(currentTab, currentFile,this::addTab), null, null, null));
		
		currentTab.addListener(i -> currentTabChange());
		root.setBottom(meta);
		meta.setStyle("-fx-font-size:0.9em;-fx-font-family:Consolas");
		BorderPane.setMargin(meta, new Insets(2));

		Scene scene = new Scene(root);
		stage.setScene(scene);

		stage.setHeight(session.getProperty("window.height", Screen.getPrimary().getVisualBounds().getHeight(), Double::parseDouble));
		stage.setWidth(session.getProperty("window.width", Screen.getPrimary().getVisualBounds().getWidth()/2, Double::parseDouble));

		stage.setX(session.getProperty("window.x", 0, Integer::parseInt));
		stage.setY(session.getProperty("window.y", 0, Integer::parseInt));

		stage.setTitle("TSV Viewer");
		stage.getIcons().add(new Image("icon.png"));
		stage.show();
		stage.setOnCloseRequest(e -> {
			close();
			e.consume();
		});

		for (String s : getParameters().getRaw()) 
			addTab(Paths.get(s));
	}

	private void currentTabChange() {
		Tab2 t = (Tab2) currentTab.get();
		if(t == null) {
			meta.textProperty().unbind();
			meta.setText(null);
			return;
		}
		meta.textProperty().bind(Bindings.concat("rows: ", t.rowsCountProperty()));
	}
	private void addTab(Path p) {
		if(Files.notExists(p)) {
			FxAlert.showErrorDialog(p, "File not Found", null);
			return;
		}
		Tab2 t;
		try {
			t = new Tab2(p);
		} catch (IOException e) {
			FxAlert.showErrorDialog(p, "Failed loading TSV", e);
			return;
		}
		tabPane.getTabs().add(t);
	}

	public void close() {
		for (Tab t : tabPane.getTabs()) {
			Tab2 t2 = (Tab2) t;
			if(t2.isModified()){
				try {
					ButtonType btn = 	FxAlert.alertBuilder(AlertType.CONFIRMATION)
							.header("Save?")
							.content(t2.getPath())
							.buttons(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL)
							.showAndWait().orElse(ButtonType.CANCEL);

					if(btn == ButtonType.YES)
						t2.save();
					else if(btn != ButtonType.NO)
						return;
				} catch (IOException e) {
					boolean b = FxAlert.alertBuilder(AlertType.CONFIRMATION)
							.header("Failed to save\bClose without saving")
							.content(t2.getPath())
							.expandableText(e)
							.expanded(true)
							.buttons(ButtonType.OK, ButtonType.CANCEL)
							.showAndWait()
							.map(s -> s == ButtonType.OK)
							.orElse(false);

					if(!b)
						return;
				}
			}
		}
		System.exit(0);
	}

}

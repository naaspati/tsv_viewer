package sam.tsv.viewer;

import static javafx.scene.input.KeyCode.C;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.F5;
import static javafx.scene.input.KeyCode.O;
import static javafx.scene.input.KeyCode.Q;
import static javafx.scene.input.KeyCode.S;
import static javafx.scene.input.KeyCode.V;
import static javafx.scene.input.KeyCombination.CONTROL_DOWN;
import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;
import static sam.fx.helpers.FxMenu.combination;
import static sam.fx.helpers.FxMenu.menuitem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import sam.fx.alert.FxAlert;
import sam.fx.popup.FxPopupShop;
import sam.tsv.viewer.tab2.Tab2;

public class MenuBar2 extends MenuBar {

	private final ReadOnlyObjectProperty<Tab> currentTab;

	private final BooleanBinding currentTabNull;
	private final BooleanBinding currentFileNull;
	private Consumer<Path> addTab;

	private final CheckMenuItem enableMenuCMI;
	private final CheckMenuItem enableCellSelectionCMI;
	private final CheckMenuItem enableEditingCMI;
	
	private final RadioMenuItem singleSelectionCMI;
	private final RadioMenuItem multipleSelectionCMI;
	private final RadioMenuItem unconstrainedCrpCMI;
	private final RadioMenuItem constrainedCrpCMI;
	
	private final ToggleGroup resizePolicy;
	private final ToggleGroup selectionModeTG;
	
	private Tab2 tab() {
		return (Tab2)currentTab.get();
	}

	public MenuBar2(ReadOnlyObjectProperty<Tab> currentTab, ObjectBinding<Path> currentFile, Consumer<Path> addTab) {
		this.currentTab = currentTab;

		this.currentTabNull = currentTab.isNull();
		this.currentFileNull = currentFile.isNull();

		this.addTab = addTab;
		
		currentTab.addListener((p, o, n) -> {
			checkAction(null);
			selectionMode(null, null, null);
			resizePolicy(null, null, null);
		});

		Menu fileMenu = new Menu("_File", null, 
				menuitem("_Open..", combination(O, SHORTCUT_DOWN), openFileHandler), 
				menuitem("From Clipboard", e -> {
					FxAlert.showMessageDialog(null, "NOT WORKING YET");
					/* TODO
					 * if(loadFromClipboard())
						stage.setTitle("");
					 */
				}), 

				menuitem("_Reload", combination(F5), e -> tab().reload(), currentFileNull), 
				menuitem("_Save", combination(S, SHORTCUT_DOWN), saveFileHandler, currentFileNull), 
				menuitem("Save As", combination(S, SHORTCUT_DOWN, SHIFT_DOWN), saveAsFileHandler, currentTabNull), 
				new SeparatorMenuItem(),  
				menuitem("E_xit", combination(Q, SHORTCUT_DOWN), e -> { App.getInstance().close(); }));

		this.enableMenuCMI = checkTabMI("Enable Menu Button");
		this.singleSelectionCMI = new RadioMenuItem("Single");
		this.multipleSelectionCMI = new RadioMenuItem("Multiple");

		selectionModeTG = toggleGrp(singleSelectionCMI,
				this::selectionMode,
				singleSelectionCMI, multipleSelectionCMI );

		Menu selectionMenu = new Menu("Selection Mode", null, singleSelectionCMI, multipleSelectionCMI);

		this.enableCellSelectionCMI = checkTabMI("Enable Cell Selection");
		enableCellSelectionCMI.setUserData(true);

		this.enableEditingCMI = checkTabMI("Enable Editing");
		enableEditingCMI.setUserData(true);

		//ColumnResizePolicy -> crp
		this.unconstrainedCrpCMI = new RadioMenuItem("Unconstrained");
		this.constrainedCrpCMI = new RadioMenuItem("Constrained");

		resizePolicy = toggleGrp(unconstrainedCrpCMI, this::resizePolicy,
				unconstrainedCrpCMI, constrainedCrpCMI);

		Menu crpMenu = new Menu("Column Resize Policy", null, constrainedCrpCMI, unconstrainedCrpCMI);
		Menu viewMenu = new Menu("_View", null, selectionMenu, crpMenu, enableMenuCMI, enableEditingCMI, enableCellSelectionCMI);

		/**
		 * 		CheckMenuItem toggleFirstRowIsHeaderMenu = new CheckMenuItem("First Row Is Header");
		toggleFirstRowIsHeaderMenu.selectedProperty().bindBidirectional(tsvModel.firstLineIsHeaderProperty());

		Menu tableMenu = new Menu("_Table", null, toggleFirstRowIsHeaderMenu);
		 */
		Menu editMenu = new Menu("Edit", null, 
				menuitem("_Copy", combination(C, CONTROL_DOWN), e -> tab().copySelected()),
				menuitem("_Paste", combination(V, CONTROL_DOWN), e -> tab().pasteClipboard()),
				menuitem("Delete Cell", combination(DELETE), e -> tab().deleteSelectedCells(), Bindings.createBooleanBinding(() -> tab() == null || !tab().isCellSelectionEnabled(), currentTab)),
				menuitem("Delete Row", combination(DELETE, CONTROL_DOWN), e -> tab().removeSeletedRows())
				);
		
		editMenu.disableProperty().bind(currentTabNull);
		viewMenu.disableProperty().bind(currentTabNull);

		setUseSystemMenuBar(true);
		getMenus().addAll(fileMenu, editMenu, viewMenu);//TODO , tableMenu);
	}
	private void selectionMode(ObservableValue<? extends Toggle> p, Toggle o, Toggle n) {
		Tab2 t = tab();
		if(t == null) return;
		
		singleSelectionCMI.setSelected(n == singleSelectionCMI);
		multipleSelectionCMI.setSelected(n == multipleSelectionCMI);
		
		if(n == null)
			selectionModeTG.selectToggle(t.getSelectionMode() == SelectionMode.SINGLE ? singleSelectionCMI : multipleSelectionCMI);
		else
			t.setSelectionMode(n == singleSelectionCMI ? SelectionMode.SINGLE : SelectionMode.MULTIPLE);
	}
	private void resizePolicy(ObservableValue<? extends Toggle> p, Toggle o, Toggle n) {
		Tab2 t = tab();
		if(t == null) return;
		
		unconstrainedCrpCMI.setSelected(n == unconstrainedCrpCMI);
		constrainedCrpCMI.setSelected(n == constrainedCrpCMI);
		
		if(n == null)
			resizePolicy.selectToggle(t.getResizePolicy() == TableView.UNCONSTRAINED_RESIZE_POLICY ? unconstrainedCrpCMI  : constrainedCrpCMI);
		else 
			t.setColumnResizePolicy(n == unconstrainedCrpCMI ?  TableView.UNCONSTRAINED_RESIZE_POLICY : TableView.CONSTRAINED_RESIZE_POLICY);
	}
	private ToggleGroup toggleGrp(RadioMenuItem selected, ChangeListener<Toggle> listner, RadioMenuItem...items) {
		ToggleGroup g = new ToggleGroup();
		g.setUserData(selected);
		g.getToggles().addAll(items);
		for (RadioMenuItem r : items) 
			r.setToggleGroup(g);
		g.selectToggle(selected);
		g.selectedToggleProperty().addListener(listner);
		return g;
	}

	private CheckMenuItem checkTabMI(String string) {
		CheckMenuItem e = new CheckMenuItem(string);
		e.setOnAction(this::checkAction);
		e.disableProperty().bind(currentTabNull);
		return e;
	}

	private void checkAction(ActionEvent e) {
		Tab2 t = tab();
		if(t == null) return;
		
		if(e == null) {
			enableMenuCMI.setSelected(t.isTableMenuButtonVisible());
			enableEditingCMI.setSelected(t.isEditable());
			enableCellSelectionCMI.setSelected(t.isCellSelectionEnabled());
			return ;
		}
		Object src = e.getSource();
		boolean b = ((CheckMenuItem)src).isSelected();  
		
		if(src == enableMenuCMI)
			t.setTableMenuButtonVisible(b);
		else if(src == enableEditingCMI)
			t.setEditable(b);
		else if(src == enableCellSelectionCMI)
			t.setCellSelectionEnabled(b);
	}

	/**
	 * 	EventHandler<ActionEvent> toggleFirstRowIsHeaderHandler  = e -> {
		data.setFirstRowIsHeader(firstRowIsHeader.get());
		table.getItems().setAll(data.getData());
		table.getColumns().forEach(c -> c.setText(data.isFirstRowHeader() ? data.getColumnName((int)c.getUserData()) : String.valueOf((char)('A'+((int)c.getUserData())))));
	};
	 */

	final EventHandler<ActionEvent> saveFileHandler  = e -> {
		try {
			tab().save();
			FxPopupShop.showHidePopup("File saved", 1500);
		} catch (IOException e11) {
			FxAlert.showErrorDialog(null, "Error while saving file", e11);
		}
	};

	final EventHandler<ActionEvent> saveAsFileHandler   = e -> {
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().addAll(new ExtensionFilter(" File", "*.tsv"), new ExtensionFilter("All Files", "*.*"));
		chooser.setTitle("Save As");

		File file = chooser.showSaveDialog(App.getStage());
		if(file == null)
			FxPopupShop.showHidePopup("cancelled", 1000);
		else{
			try {
				tab().save(file.toPath());
				FxPopupShop.showHidePopup("File saved", 1000);	
			} catch (IOException e1) {
				FxAlert.showErrorDialog(null, "Error  while file saving", e1);
			}
		}
	};
	final EventHandler<ActionEvent> openFileHandler = e -> {
		FileChooser chooser = new FileChooser();
		chooser.getExtensionFilters().addAll(new ExtensionFilter(" File", "*.tsv"), new ExtensionFilter("All Files", "*.*"));
		chooser.setTitle("Open File");

		File file = chooser.showOpenDialog(App.getStage());

		if(file != null)
			addTab.accept(file.toPath());
		else
			FxPopupShop.showHidePopup("Cancelled", 1500);
	};
}

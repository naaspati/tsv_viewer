package sam.tsv.viewer.tab2;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.collections.ObservableList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.ResizeFeatures;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import sam.fx.alert.FxAlert;
import sam.fx.clipboard.FxClipboard;
import sam.fx.popup.FxPopupShop;
import sam.tsv.Row;
import sam.tsv.Tsv;
import sam.tsv.viewer.Editor;
import sam.tsv.viewer.tab2.TableCol.RowCol;

public class Tab2 extends Tab {
	private final TableView<Row> table = new TableView<>();
	private final TableViewSelectionModel<Row> selectionModel = table.getSelectionModel();
	private final ObservableList<TableColumn<Row, ?>> columns = table.getColumns(); 
	private Tsv tsv;

	private ReadOnlyBooleanWrapper modifiedb = new ReadOnlyBooleanWrapper();
	//private ReadOnlyBooleanWrapper modified = new ReadOnlyBooleanWrapper();

	private static int counter = 1;

	public Tab2(Path file) throws IOException {
		table.setEditable(true);
		table.getSelectionModel().setCellSelectionEnabled(true);
		table.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
			if(e.isControlDown()) {
				switch (e.getCode()) {
					case V:
						pasteAction();
						e.consume();
						return;
					case C:
						copySelected();
						e.consume();
						return;
					case DELETE:
						removeSeletedRows();
						e.consume();
						return;
					default:
						break;
				}	
			}

			switch (e.getCode()) {
				case DELETE:
					clearSelectedCell();
					e.consume();
					return;
				case ENTER:
					edit();
					e.consume();
					return;
				default:
					break;
			}
		});
		table.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
			if(e.getClickCount() == 2)
				edit();
		});
		load(file);
		setContent(table);
	}

	private void edit() {
		Optional<RowCol> c = firstSelectedCell();
		if(!c.isPresent() ) {
			FxPopupShop.showHidePopup("nothing selected", 1500);
			return;
		}
		RowCol r = c.get();
		String s = Editor.getInstance().get(r.get());
		if(s != null)
			r.set(s);
	}
	private void clearSelectedCell() {
		selectionModel.getSelectedCells()
		.forEach(e -> rowCol(e).set(null));
	}
	private Optional<RowCol> firstSelectedCell() {
		return Optional.of(selectionModel.getSelectedCells())
				.filter(e -> !e.isEmpty())
				.map(e -> e.get(0))
				.map(this::rowCol);
	}
	public void pasteAction() {
		firstSelectedCell()
		.ifPresent(e -> {
			String s = FxClipboard.getString();
			if(s == null)
				FxPopupShop.showHidePopup("no data in clipboard", 1500);
			else
				((RowCol)e).set(s);
		});
	}

	private final Runnable modified_listener = () -> modifiedb.set(true);

	private void columnsInit() {
		modifiedb.set(false);
		columns.clear();

		String[] columnNames = tsv.getColumnNames();
		int len = columnNames.length;

		for (int i = 0; i < len; i++) 
			columns.add(new TableCol(columnNames[i], i, modified_listener, len));

		setName();
		Platform.runLater(() -> table.refresh());
	}

	private String altname;
	public void setName() {
		Path  p = tsv.getPath();
		String name ;
		if(p == null) 
			name = (altname = altname != null ? altname :  "New "+(counter++));
		else
			name = p.getFileName().toString();

		setText(name); 
	}
	public Path getPath() {
		return tsv.getPath();
	}
	public void save() throws IOException {
		tsv.save();
		FxPopupShop.showHidePopup("saved", 1500);
		System.out.println("saved: "+tsv.getPath());
		clearModified();
	}
	public void save(Path path) throws IOException {
		tsv.save(path);
		System.out.println("saved: "+path);
		setName();
		clearModified();
	}
	private void clearModified() {
		columns.forEach(t -> ((TableCol)t).clearModified());
		Platform.runLater(() -> modifiedb.set(false));
	}
	public void removeSeletedRows() {
		Optional.of(selectionModel.getSelectedItems())
		.filter(l -> !l.isEmpty())
		.map(ArrayList::new)
		.ifPresent(list -> {
			selectionModel.clearSelection();
			table.getItems().removeAll(list);
			modifiedb.set(true);	
		});
	}
	@SuppressWarnings("rawtypes")
	public void copySelected() {
		if(selectionModel.isCellSelectionEnabled()) {
			List<TablePosition> list = selectionModel.getSelectedCells();
			if(list.isEmpty())
				return;
			if(list.size() == 1) {
				copyToClipboard(rowCol(list.get(0)).get());
				return;
			}

			Map<Integer, List<Integer>> map = 
					selectionModel.getSelectedCells().stream()
					.collect(Collectors.groupingBy(TablePosition::getRow, Collectors.mapping(TablePosition::getColumn, Collectors.toList())));

			String[][] strs = new String[map.size()][map.keySet().stream().mapToInt(i -> table.getItems().get(i).size()).max().getAsInt()];

			int index[] = {0};
			map.forEach((rowIndex, columns) -> {
				String[] str = strs[index[0]++];
				Row row = table.getItems().get(rowIndex);
				for (int i : columns)
					str[i] = row.get(i);
			});
			boolean[] nulls = new boolean[strs[0].length];

			for (String[] s : strs) {
				for (int i = 0; i < s.length; i++)
					nulls[i] = nulls[i] || s[i] != null;  
			}

			StringBuilder sb = new StringBuilder();

			for (String[] s : strs) {
				for (int i = 0; i < s.length; i++) {
					if(nulls[i])
						sb.append(s[i] == null ? "" : s[i]).append('\t');
				}
				if(sb.length() > 0 && sb.charAt(sb.length() - 1)  == '\t')
					sb.setLength(sb.length() - 1);
				sb.append('\n');
			}
			if(sb.length() > 0 && sb.charAt(sb.length() - 1)  == '\n')
				sb.setLength(sb.length() - 1);

			copyToClipboard(sb.toString());
		}
		else {
			copyToClipboard(selectionModel.getSelectedItems()
					.stream()
					.reduce(new StringBuilder(), (sb, r) -> r.join("\t", "", sb).append('\n'), StringBuilder::append).toString());
		}
	}
	private RowCol rowCol(@SuppressWarnings("rawtypes") TablePosition t) {
		return ((TableCol) t.getTableColumn()).getCellData(t.getRow());
	}
	private static void copyToClipboard(String data) {
		ClipboardContent cc = new ClipboardContent();
		cc.putString(data);
		if(Clipboard.getSystemClipboard().setContent(cc))
			FxPopupShop.showHidePopup("copied", 1500);
	}
	private void load(Path file) throws IOException {
		if(Files.notExists(file))
			throw new FileNotFoundException(file.toString());

		table.getItems().clear();
		this.tsv = Tsv.builder().rowStore(table.getItems()).parse(file);
		columnsInit();
	}
	public void reload() {
		try {
			load(getPath());
		} catch (IOException e) {
			FxAlert.showErrorDialog(getPath(), "Failed to reload", e);
		}
	}
	public void setTableMenuButtonVisible(boolean b) {
		table.setTableMenuButtonVisible(b);
	}
	public boolean isTableMenuButtonVisible() {
		return table.isTableMenuButtonVisible();
	}
	public void setEditable(boolean b) {
		table.setEditable(b);
	}
	public boolean isEditable() {
		return table.isEditable();
	}
	public void setCellSelectionEnabled(boolean b) {
		table.getSelectionModel().setCellSelectionEnabled(b);
	}
	public boolean isCellSelectionEnabled() {
		return table.getSelectionModel().isCellSelectionEnabled();
	}
	@SuppressWarnings("rawtypes")
	public void setColumnResizePolicy(Callback<ResizeFeatures, Boolean> object) {
		table.setColumnResizePolicy(object);
	}
	@SuppressWarnings("rawtypes")
	public Callback<ResizeFeatures, Boolean> getResizePolicy(){
		return table.getColumnResizePolicy();
	}

	public SelectionMode getSelectionMode() {
		return selectionModel.getSelectionMode();
	}
	public void setSelectionMode(SelectionMode mode) {
		selectionModel.setSelectionMode(mode);
	}
	public ReadOnlyBooleanProperty modifiedProperty() {
		return modifiedb.getReadOnlyProperty();
	}
	public void addRow() {
		tsv.addRow(tsv.rowBuilder().build());
		table.getSelectionModel().selectLast();
		table.scrollTo(table.getItems().size() - 1);
		table.getFocusModel().focus(table.getItems().size() - 1, columns.get(0));
		modifiedb.set(true);
	}

	public boolean isModified() {
		return modifiedb.get();
	}
	public int rowCount() {
		return tsv.size();
	}

	public IntegerBinding rowsCountProperty() {
		return Bindings.size(table.getItems());
	}
}
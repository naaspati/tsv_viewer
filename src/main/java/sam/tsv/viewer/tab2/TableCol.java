package sam.tsv.viewer.tab2;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.function.Function;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import sam.tsv.Row;
import sam.tsv.viewer.tab2.TableCol.RowCol;

public class TableCol extends TableColumn<Row, RowCol> {
	private final Runnable onModified;
	private boolean modified;
	final int column_count, col_index;
	private final IdentityHashMap<Row, Loader> rows2 = new IdentityHashMap<>(); 

	public TableCol(String title, int index, Runnable onModified, int column_count) {
		super(title);
		this.onModified = onModified;
		this.col_index = index;
		this.column_count  = column_count;

		setCellFactory(column -> new TableCell2());
		Function<Row, Loader> computer = Loader::new;  
		setCellValueFactory(cdata -> rows2.computeIfAbsent(cdata.getValue(), computer).get(index));
	}
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	private class Loader {
		final Row row;
		SimpleObjectProperty[] cols;
		
		public Loader(Row row) {
			this.row = row;
		}
		public SimpleObjectProperty<RowCol> get(int index) {
			if(cols == null)
				cols = new SimpleObjectProperty[column_count];
			if(index >= cols.length)
				cols = Arrays.copyOf(cols, index+1);
			
			SimpleObjectProperty o = cols[index];
			if(o != null)
				return o;
			
			return cols[index] = new SimpleObjectProperty(new RowCol(row));
		}
		
	}
	

	public static class TableCell2 extends TableCell<Row, RowCol> {
		{
			setWrapText(true);
		}

		@Override
		protected void updateItem(RowCol item, boolean empty) {
			super.updateItem(item, empty);
			
			if(empty || item == null) {
				setText(null);
				setUserData(null);
			} else {
				item.setView(this);
				String s = item.get();
				int n = s == null ? -1 : s.indexOf('\n');
				if(n >= 0) {
					if(s.length() > 1 && s.charAt(n - 1) == '\r')
						n--;
					s = s.substring(0, n).concat("...");
				}
				setText(s);	
			}
		}
	}
	
	class RowCol {
		public final Row row;
		private TableCell2 view;
		
		public RowCol(Row row) {
			this.row = row;
		}
		public String get() {
			return row.get(col_index);
		}
		public void set(String value) {
			if(Objects.equals(value, get()))
				return;
			
			row.set(col_index, value);
			if(view != null && view.getUserData() == this)
				view.setText(value);
			
			modified();
		}
		
		private void setView(TableCell2 view) {
			if(this.view != null && this.view.getUserData() == this)
				this.view.setUserData(null);
			
			this.view = view;
			this.view.setUserData(this);
		}
	}
	
	public void modified() {
		if(modified)
			return;
		modified = true;
		onModified.run();
	}
	void clearModified() {
		modified = false;
	}
}

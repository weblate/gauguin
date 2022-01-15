package com.holokenmod.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.holokenmod.Grid;
import com.holokenmod.GridCell;
import com.holokenmod.R;
import com.holokenmod.SaveGame;
import com.holokenmod.Theme;
import com.holokenmod.Utils;
import com.holokenmod.options.ApplicationPreferences;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

public class SaveGameListAdapter extends BaseAdapter {
	
	public final ArrayList<File> mGameFiles;
	private final LayoutInflater inflater;
	private final SaveGameListActivity mContext;
	
	public SaveGameListAdapter(final SaveGameListActivity context) {
		this.inflater = LayoutInflater.from(context);
		this.mContext = context;
		this.mGameFiles = new ArrayList<>();
		this.refreshFiles();
	}
	
	public void refreshFiles() {
		this.mGameFiles.clear();
		this.mGameFiles.addAll(mContext.getSaveGameFiles());
		
		Collections.sort(this.mGameFiles, new SortSavedGames());
	}
	
	public int getCount() {
		return this.mGameFiles.size();
	}
	
	public Object getItem(final int arg0) {
		return this.mGameFiles.get(arg0).getName();
	}
	
	public long getItemId(final int position) {
		return position;
	}
	
	public View getView(final int position, View convertView, final ViewGroup parent) {
		convertView = inflater.inflate(R.layout.object_savegame, null);
		
		final GridUI gridUI = convertView.findViewById(R.id.saveGridView);
		final TextView gametitle = convertView.findViewById(R.id.saveGameTitle);
		final TextView datetime = convertView.findViewById(R.id.saveDateTime);
		
		final File saveFile = this.mGameFiles.get(position);
		
		final SaveGame saver = new SaveGame(saveFile);
		try {
			saver.restore(gridUI);
		} catch (final Exception e) {
			// Error, delete the file.
			saveFile.delete();
			return convertView;
		}
		
		final Theme theme = ApplicationPreferences.getInstance().getTheme();
		
		Grid grid = gridUI.getGrid();
		
		grid.setActive(false);
		gridUI.setTheme(theme);
  
		for (final GridCell cell : grid.getCells()) {
            cell.setSelected(false);
        }
		
		final long millis = grid.getPlayTime();
		gametitle.setText(grid.getGridSize().toString() + " " + Utils.convertTimetoStr(millis));
		
		final Calendar gameDateTime = Calendar.getInstance();
		gameDateTime.setTimeInMillis(grid.getCreationDate());
		datetime.setText("" + DateFormat.getDateTimeInstance(
				DateFormat.MEDIUM, DateFormat.SHORT).format(grid.getCreationDate()));
		
		final MaterialButton loadButton = convertView.findViewById(R.id.button_play);
		loadButton.setOnClickListener(v -> mContext.loadSaveGame(saveFile));
		
		final MaterialButton deleteButton = convertView.findViewById(R.id.button_delete);
		deleteButton.setOnClickListener(v -> mContext.deleteGameDialog(saveFile));
		
		return convertView;
	}
	
	private static class SortSavedGames implements Comparator<File> {
		long save1 = 0;
		long save2 = 0;
		
		public int compare(final File object1, final File object2) {
			try {
				save1 = new SaveGame(object1).ReadDate();
				save2 = new SaveGame(object2).ReadDate();
			} catch (final Exception e) {
				//
			}
			return (int) Math.signum(save2 - save1);
		}
		
	}
}
package jp.syoboi.android.garaponmate.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ProgramList extends ArrayList<Program> {
	/**
	 *
	 */
	private static final long serialVersionUID = -8617447393412340471L;

	int findByGtvId(String gtvid) {
		for (int j=size()-1; j>=0; j--) {
			if (gtvid.equals(get(j).gtvid)) {
				return j;
			}
		}
		return -1;
	}

	public void merge(ProgramList program) {
		for (Program p: program) {
			if (findByGtvId(p.gtvid) == -1) {
				add(p);
			}
		}

		Collections.sort(this, new Comparator<Program>() {
			@Override
			public int compare(Program lhs, Program rhs) {
				return (int)(rhs.startdate - lhs.startdate);
			}
		});
	}
}

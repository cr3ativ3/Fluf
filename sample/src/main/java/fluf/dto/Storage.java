package fluf.dto;

import java.util.Collection;

public class Storage<T> {

	private Collection<T> itemsInStorage;

	public Storage(Collection<T> items) {
		this.itemsInStorage = items;
	}
}
package com.seizonsenryaku.testwidget;

import java.util.ArrayList;
import java.util.List;

import android.util.SparseArray;

public class Trie<T> {

	private class Node {
		SparseArray<Node> children;
		public T object;

		public Node() {
			children = new SparseArray<Node>();
		}

	}

	private Node root;

	Trie() {
		root = new Node();
	}

	public void put(CharSequence charSequence, T object) {
		addRec(charSequence, root, object);
	}

	public T get(CharSequence charSequence) {
		return getRec(charSequence, root);
	}

	public List<T> getAllStartingWith(CharSequence charSequence) {
		// Log.d("TRIE", "Starting with " + charSequence);
		return getAllStartingWithRec(charSequence, root, new ArrayList<T>());
	}

	private List<T> getAllStartingWithRec(CharSequence charSequence,
			Node ancestor, List<T> list) {
		int length = charSequence.length();
		if (length > 0) {
			char currentLetter = charSequence.charAt(0);
			Node child = ancestor.children.get(currentLetter);
			if (child == null) {
				// Log.d("TRIE", "DEAD END");
				return list;
			}
			return getAllStartingWithRec(charSequence.subSequence(1, length),
					child, list);
		} else {
			if (ancestor.object != null) {
                if(!list.contains(ancestor.object))
				    list.add(ancestor.object);
				// Log.d("TRIE", charSequence.toString());
			} else {
				// Log.d("TRIE", "NULL");
			}
			int numOfChildren = ancestor.children.size();

			for (int childIndex = 0; childIndex < numOfChildren; childIndex++) {
				getAllStartingWithRec(charSequence,
						ancestor.children.valueAt(childIndex), list);
			}
			return list;
		}
	}

	private T getRec(CharSequence charSequence, Node ancestor) {
		int length = charSequence.length();
		if (length > 0) {
			char currentLetter = charSequence.charAt(0);
			Node child = ancestor.children.get(currentLetter);
			if (child == null) {
				return null;
			}
			return getRec(charSequence.subSequence(1, length), child);
		} else {
			return ancestor.object;
		}

	}

	private void addRec(CharSequence charSequence, Node node, T object) {
		int length = charSequence.length();
		if (length > 0) {
			char currentLetter = charSequence.charAt(0);
			Node child = node.children.get(currentLetter);
			if (child == null) {
				child = new Node();
				node.children.put(currentLetter, child);
			}
			addRec(charSequence.subSequence(1, length), child, object);
		} else {
			if (node.object == null) {
				node.object = object;
			} else {
				addRec(charSequence + " ", node, object);
			}
		}

	}
}

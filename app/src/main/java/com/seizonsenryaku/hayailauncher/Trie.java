package com.seizonsenryaku.hayailauncher;

import android.util.Log;
import android.util.SparseArray;

import java.util.HashSet;

public class Trie<T> {

	private final class Node {
		final SparseArray<Node> children;
		public T object;

		public Node() {
			children = new SparseArray<>();
		}

	}

	private final Node root;

	public Trie() {
		root = new Node();
	}

	public void put(CharSequence charSequence, T object) {
		Log.d("TRIE",charSequence.toString());
		addRec(charSequence, root, object);
	}

	public T get(CharSequence charSequence) {
		final Node node = getNodeRec(charSequence, root);
		return node!=null ? node.object : null;
	}

	public boolean remove(CharSequence charSequence, T object){
		Node ancestor=root;
		Node node;
		do{
			node=getNodeRec(charSequence, ancestor);
			if(node==null)
				return false;
			if(node.object==object)
				node.object=null;
			else {
				charSequence = " ";
				ancestor=node;
			}
		}while(node.object != null);
		return true;
	}

	public HashSet<T> getAllStartingWith(CharSequence charSequence) {
        return getAllStartingWithRec(charSequence, root, new HashSet<T>());
	}

	private HashSet<T> getAllStartingWithRec(CharSequence charSequence,
			Node ancestor, HashSet<T> list) {
		final int length = charSequence.length();
		if (length > 0) {
			final char currentLetter = charSequence.charAt(0);
			final Node child = ancestor.children.get(currentLetter);
			if (child == null) {
				return list;
			}
			return getAllStartingWithRec(charSequence.subSequence(1, length),
					child, list);
		} else {
			if (ancestor.object != null) {
				    list.add(ancestor.object);
			}
			final int numOfChildren = ancestor.children.size();

			for (int childIndex = 0; childIndex < numOfChildren; childIndex++) {
				getAllStartingWithRec(charSequence,
						ancestor.children.valueAt(childIndex), list);
			}
			return list;
		}
	}

	private Node getNodeRec(CharSequence charSequence, Node ancestor) {
		final int length = charSequence.length();
		if (length > 0) {
			final char currentLetter = charSequence.charAt(0);
			final Node child = ancestor.children.get(currentLetter);
			if (child == null) {
				return null;
			}
			return getNodeRec(charSequence.subSequence(1, length), child);
		} else {
			return ancestor;
		}

	}

	private void addRec(CharSequence charSequence, Node node, T object) {
		final int length = charSequence.length();
		if (length > 0) {
			final char currentLetter = charSequence.charAt(0);
            Node child = node.children.get(currentLetter);
			if (child == null) {
				child = new Node();
				node.children.put(currentLetter, child);
			}
			addRec(charSequence.subSequence(1, length), child, object);
		} else {
			if (node.object == null) {
				node.object = object;
			} else if(node.object != object){
				addRec(charSequence + " ", node, object);
			}
		}

	}
}

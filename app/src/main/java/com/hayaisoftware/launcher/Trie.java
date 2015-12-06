/*  Copyright 2015 Hayai Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hayaisoftware.launcher;

import android.util.Log;
import android.util.SparseArray;

import java.util.HashSet;

public class Trie<T> {

	private final Node mRoot;

	public Trie() {
		mRoot = new Node();
	}

	public void put(final CharSequence charSequence, T object) {
		Log.d("TRIE",charSequence.toString());
		addRec(charSequence, mRoot, object);
	}

	public T get(final CharSequence charSequence) {
		final Node node = getNodeRec(charSequence, mRoot);
		return node!=null ? node.object : null;
	}

	public boolean remove(CharSequence charSequence, final T object) {
		Node ancestor = mRoot;
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

	public HashSet<T> getAllStartingWith(final CharSequence charSequence) {
		return getAllStartingWithRec(charSequence, mRoot, new HashSet<T>());
	}

	private HashSet<T> getAllStartingWithRec(final CharSequence charSequence,
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

	private Node getNodeRec(final CharSequence charSequence, final Node ancestor) {
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

	private void addRec(final CharSequence charSequence, final Node node, final T object) {
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

	private final class Node {
		final SparseArray<Node> children;
		public T object;

		public Node() {
			children = new SparseArray<>();
		}

	}
}

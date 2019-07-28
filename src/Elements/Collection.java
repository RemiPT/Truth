package Elements;

import java.util.ArrayList;


public class Collection extends Term {
	
	ArrayList<Term> items;
	private boolean cartesian;
	public Collection () {
		super();
		items = new ArrayList<Term>();
		size = 0;
		iscollection = true;
		cartesian = false;
	}
	
	public Term get (int i) { return items.get(i);  }
	
	public boolean equalsString(String s) { return false; }
	public boolean equals (Term other) {
		if (!other.isCollection()) return false;
		Collection c = (Collection) other;
		
		int len = this.size;
		if (c.size != len) return false;
		
		for (int i=0; i<len; i++) {
			if (!other.get(i).equals(this.get(i))) return false;
		}
		return true;
	}
	
	public boolean isOperator() { return false; }
	public boolean isShallow() { return false; }
	public void flatten() {}
	
	public void addTerm (Term t) { size++; items.add(t); }
	
	public Collection copy() {
		Collection output = new Collection();
		for (Term t: items) output.addTerm(t.copy());
		return output;
	}
	public String toString() {
		String link = ", ";
		if (cartesian) link = " x ";
		String output = "(";
		for (Term t: items) {
			if (!t.isShallow() && !(t.getDisposition() == Disp.C)) output += "(" + t.toString() + ")" + link;
			else output += t.toString() + link;
		}
		return output.substring(0, output.length()-link.length()) + ")";
	}
	
	
	public boolean isCartesian() { return cartesian; }
	public void setCartesian (boolean bool) { cartesian = bool; }
	
	public void embedVariableNames(String head, ArrayList<String> vars) {
		for (Term x: items) x.embedVariableNames(head, vars);
	}

	public void removeEmbeding() {
		for (Term x: items) x.removeEmbeding();
	}
	
	static public ArrayList<Statement> extractDiffArray(Term tthm, Term tprop) throws ExceptionTheoremNotApplicable {
		ArrayList<Statement> result = new ArrayList<Statement>();
		Collection c1 = (Collection) tthm;
		
		if (tprop.getDisposition() != Disp.C) throw new ExceptionTheoremNotApplicable();
		Collection c2 = (Collection) tprop;
		
		if (c1.size != c2.size) throw new ExceptionTheoremNotApplicable();
		for (int i=0; i<c1.size; i++) {
			result.addAll(Term.extractDiffArray(c1.get(i), c2.get(i)));
		}
		return result;
	}
	
	static boolean extractDiffInner(Term t1, Term t2, Link clink, DifferencesLedger dL) {
		if (!(clink.equals("="))) return false;
		Collection c1 = (Collection) t1;
		Collection c2 = (Collection) t2;
		if (c1.size != c2.size) return false;
		
		int singleDifference = -1;
		for (int i=0; i<c1.size; i++) {
			if (!c1.get(i).equals(c2.get(i))) {
				if (singleDifference == -1) singleDifference = i;
				else return false; // Won't deal with several differences
			}
		}
		if (singleDifference == -1) return true;
		else return Term.extractDiffInner(c1.get(singleDifference), c2.get(singleDifference), clink, dL);
		
	}
	
	static public Permutations permute (Collection c) {
		Permutations perm = new Permutations();
		
		ArrayList<Collection> colls = new ArrayList<Collection>();
		colls.add(new Collection());
		for (Term t: c.items) {
			ArrayList<Collection> newcolls = new ArrayList<Collection>();
			for (Term p: Term.permute(t).vs) {
				for (Collection col: colls) {
					Collection newcol = col.copy();
					newcol.addTerm(p);
					newcolls.add(newcol);
				}
			}
			colls = newcolls;
		}
		
		for (Term t: colls) perm.add(t);
		return perm;
	}
	
	
	/* ====================================
	 * Collection functions
	 */
	
	/*
	public boolean isElementOfSet (Set S) {
		
	}*/
}
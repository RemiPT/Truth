package Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Stack;

import Operation.Op;
import Operation.Operator;

public class Term {

	public static enum Disp {SET, TOT, QTT, OT, F, C, ERR};
	private Disp disp;
	protected boolean iscollection = false;
	protected boolean isoperator = false;
	
	public Term[] v;
	public int size;
	public String s;

	public Term() {
		size = 0;
		disp = null;
	}
	public Term(String s) {
		this.s = s;
		size = 0;
		disp = null;
	}
	
	public static Term makeNewTerm (String s) {
		Operator op = Op.getOperator(s);
		if (op == null) return new Term(s);
		else return op;
	}
	
	public boolean isCollection () {return iscollection; }
	public boolean isOperator () {return isoperator; }
	
	public void addTerm(Term t) {
		Term[] oldv = v;
		v = new Term[++size];
		if (oldv != null) {
			for (int i=0; i<oldv.length; i++) v[i] = oldv[i];
		}
		v[size-1] = t;
		disp = null;
	}
	
	public Term get(int i) {
		try {
			return v[i];
		} catch (Exception e) {
			System.out.println("[[[ FATAL ]]] Tried to get index " + i + " from length " + size + " in term: " + v.toString());
			throw new IndexOutOfBoundsException();
		}
	}
	
	// I don't want 'Term' to be iterable...
	/*
	public class TermArrayIterator implements Iterable<Term> {
		Term[] v;
		public TermArrayIterator (Term[] v) { this.v = v; }
		public Iterator<Term> iterator() {
			return v;
		}
	}
	public TermArrayIterator iterator() {
		return new TermArrayIterator(v);
	}*/
	
	public boolean equalsString(String s) {
		if (!isShallow()) return false;
		return this.s.equals(s);
	}
	
	/*
	public boolean isOperator() {
		if (isShallow() && Operator.isOperator(s)) return true;
		return false;
	}	*/
	
	public boolean equals(Term t) {
		Disp disp = this.getDisposition();
		if (t.getDisposition() != disp) return false;
		if (disp == Disp.F) {
			if (this.s == null && t.s == null) return true;
			return s.equals(t.s);
		} else {
			for (Term tperm: Term.permute(t).vs) {
				boolean result = true;
				for (int i=0; i<size; i++) result = result && (get(i).equals(tperm.get(i)));
				if (result) return true;
			}
			return false;
		}
	}
	
	public boolean isShallow() {
		flatten();
		return v == null;
	}
	public void flatten() {
		// Are we able to obtain a shallow term?
		if (size==1) {
			Term inner = get(0);
			disp = null;
			if (inner.size == 0) {
				v = null;
				s = inner.s;
				size = 0;
			} else {
				v = inner.v;
				size = inner.size;
				flatten();
			}
		}
	}	
	
	/* TOT := [Term, Operator, Term]   QTT := [Quantifier, Term, Term]  OT := [Operator, Term]  F := [Term] (flat)   ERR := error  */
	public Disp getDisposition() {
		if (disp == null) disp = computeDisposition();
		return disp;
	}
	
	private Disp computeDisposition() {
		if (isShallow() || isOperator()) return Disp.F;
		if (isCollection()) return Disp.C;
		
		Term t1 = get(0);
		Term t2 = get(1);
		if (size == 2) {
			if (t1.isOperator()) {
				if (((Operator) t1).isUnary() && !t2.isOperator()) return Disp.OT;
			}
		} else if (size == 3) {
			Term t3 = get(2);
			if (t2.isOperator() && ((Operator) t2).isBinary() && !t1.isOperator() && !t3.isOperator()) {
				return Disp.TOT;
			}
			if (t1.isOperator() && !t2.isOperator() && !t3.isOperator()) {
				Operator op1 = (Operator) t1;
				if (op1.isQuantifier()) return Disp.QTT;
				if (op1.isSet()) return Disp.SET;
			}
		}
		return Disp.ERR;
	}
	
	
	public Term copy() {
		if (isShallow()) return makeNewTerm(s);
		Term copy = new Term();
		for (Term x: v) {
			copy.addTerm(x.copy());
		}
		return copy;
	}
	
	public String toString() {
		Disp disp = getDisposition();
		if (disp == Disp.F) return s;	
		if (disp == Disp.C) return toString();
		if (disp == Disp.QTT) return get(0).s + get(1).toString() + ": " + get(2).toString();
		
		String output = "";
		if (disp == Disp.ERR) output += "[disp error (size="+size+")] ";
		
		for (Term x: v) {
			Disp innerdisp = x.getDisposition();
			if (innerdisp == Disp.F) output += x.s + " ";
			else if (innerdisp == Disp.C) output += x.toString() + " ";
			else if (innerdisp == Disp.OT) {
				output += x.get(0).s + " ";
				Term secondterm = x.get(1);
				Disp seconddisp = secondterm.getDisposition();
				if (seconddisp != Disp.F && seconddisp != Disp.OT) output += "(" + secondterm.toString() + ") ";
				else output += secondterm.toString() + " ";
			}
			else output += "(" + x.toString() + ") ";
		}
		return removeLastSpace(output);
	}
	
	private String removeLastSpace(String s) {
		int len = s.length();
		if (len == 0) return "";
		return "" + s.substring(0, len-1);
	}
	
	
	public void embedVariableNames(String head, ArrayList<String> vars) {
		if (isShallow()) {
			//if (!s.startsWith(head) && !s.startsWith("\\") && !Operator.isOperator(s)) s = head + s;
			if (!s.startsWith(head) && vars.contains(s)) s = head + s;
		} else {
			for (Term x: v) x.embedVariableNames(head, vars);
		}
	}

	public void removeEmbeding() {
		if (isShallow()) {
			String[] sections = s.split("\\.");
			s = sections[sections.length-1];
		} else {
			for (Term x: v) x.removeEmbeding();
		}
	}

	
	static private Term glueTerms(Term[] ts) {
		Term output = new Term();
		for (Term x: ts) {
			output.addTerm(x);
		}
		return output;
	}
	
	
	static private Term reduce (Term termarray) {
		if (termarray.isShallow() || termarray.isCollection()) {
			return termarray;
		}
		Term result = new Term();
		LinkedList<Term> output = new LinkedList<Term>();
		
		Stack<Term> input = new Stack<Term>();
		for (Term inner: termarray.v) input.add(inner);

		while (!input.isEmpty()) {
			Term ith = input.pop(); // Reversed order
			if (ith.isOperator()) {
				if (ith.equals(Op.forall)) {
					// TODO collection as syntatxic sugar
					output = extractQuantifiers(output, Op.forall, "\\follows");
					if (output == null) return null;
					
				} else if (ith.equals(Op.exists)) {
					output = extractQuantifiers(output, Op.exists, "\\suchthat");
					if (output == null) return null;
					
				} else if (ith.equals(Op.not)) {
					Term notterm = new Term();
					notterm.addTerm(ith);
					notterm.addTerm(output.pop());
					output.addFirst(notterm);
					
				} else output.addFirst(ith);
			} else output.addFirst(ith);
		}
		
		for (Term t: output) result.addTerm(t);
		return result;
	}
	
	static private LinkedList<Term> extractQuantifiers (LinkedList<Term> output, Operator quantop, String token) {
		try {
			int followsPos=1;
			while (!output.get(followsPos).equalsString(token)) followsPos++;
			Term quantterm = new Term();
			Term condition = new Term();
			Term proposition = new Term();
			
			for (int i=0; i<followsPos; i++) condition.addTerm(output.pop());
			output.pop(); // removal of the "follows" term
			while (!output.isEmpty()) proposition.addTerm(output.pop());
						
			quantterm.addTerm(quantop.copy());
			quantterm.addTerm(condition);
			quantterm.addTerm(proposition);
			output.addFirst(quantterm);
			return output;
		} catch (IndexOutOfBoundsException e) { System.out.println("Couldn't find '" + token + "' token"); return null;}
	}
	
	/*
	 * Returns a term structure (list of other terms, never immediatly shallow)
	 */
	static public Term compileTerms(ArrayList<String> seq) {
		//System.out.println("___");
		//for (String s: seq) System.out.println(s);
		
		Term result = new Term();
		ArrayList<String> innerBuffer = new ArrayList<String>();
		int openedParenthesis = 0;
		boolean inCollection = false;
		
		// In case of redundant parenthesis
		while (seq.get(0).equals("")) seq.remove(0);
		while (isSurroundedByParenthesis(seq)) {
			seq.set(0, seq.get(0).substring(1));
			String lastString = seq.get(seq.size()-1);
			seq.set(seq.size()-1, lastString.substring(0, lastString.length()-1));
		}
		
		if (seq.size() == 1) {
			String element = seq.get(0);
			if (isSingletonCollection(element)) return compileSingletonCollection(element);
			else return makeNewTerm(element);
		}
		
		boolean foundlink = false;
		for (String x: seq) {
			x = x.trim();
			if (openedParenthesis > 0) {
				openedParenthesis += getParenthesisDifferential(x);
				innerBuffer.add(x);
				
				if (openedParenthesis < 0) System.out.println("[FATAL] Problem with parenthesis in: " + x);
				if (openedParenthesis == 0) {
					Term innerterm;
					if (inCollection) {
						innerterm = compileCollection(innerBuffer);
						inCollection = false;
					}
					else innerterm = compileTerms(innerBuffer);
					result.addTerm(innerterm);
					innerBuffer = new ArrayList<String>();
				}
				
			} else {
				openedParenthesis += getParenthesisDifferential(x);
				if (isSingletonCollection(x)) result.addTerm(compileSingletonCollection(x));
				else if (openedParenthesis > 0) {
					if (isCollectionHeader(x)) inCollection = true;
					innerBuffer.add(x);
				}
				else {
					if (Link.isLink(x)) foundlink = true;
					result.addTerm(makeNewTerm(x));
				}
			}
		}
		
		if (!foundlink) {
			//System.out.println("Reducing: " + result.toString() + "  (" + result.size + ")");
			result = Term.reduce(result);
			//System.out.println("Reduced: " + result.toString() + "  (" + result.size + ")");
		}
		if (foundlink && result.size > 3) {
			
			Term parsed = new Term();
			for (int i=0; i<result.size; i++) {
				Term ith = result.get(i);
				if (ith.isOperator() && Link.isLink(ith.s)) {

					// Left
					Term tleft = new Term();
					for (int j=0; j<i; j++) {
						tleft.addTerm(result.get(j));
					}
					
					// Right
					Term tright = new Term();
					for (int j=i+1; j<result.size; j++) {
						tright.addTerm(result.get(j));
					}
					
					//System.out.println("LeftReducing: " + tleft.toString() + "  (" + tleft.size + ")");
					parsed.addTerm(Term.reduce(tleft));
					//System.out.println("LeftReduced: " + tleft.toString() + "  (" + tleft.size + ")");
					parsed.addTerm(result.get(i));
					//System.out.println("RightReducing: " + tright.toString() + "  (" + tright.size + ")");
					parsed.addTerm(Term.reduce(tright));
					//System.out.println("RightReduced: " + tright.toString() + "  (" + tright.size + ")");
					
				}
			}
			return parsed;
		}
		return result;
	}
	
	static private boolean isCollectionHeader (String s) {
		return s.startsWith("\\set(") || s.startsWith("\\cartprod(");
	}
	static private boolean isSingletonCollection (String s) {
		return isCollectionHeader(s) && s.endsWith(")");
	}
	static private Collection compileSingletonCollection (String s) {
		ArrayList<ArrayList<String>> collections = new ArrayList<ArrayList<String>>();
		ArrayList<String> innercoll = new ArrayList<String>();
		innercoll.add(s);
		collections.add(innercoll);
		return compileCollectionParsed(collections);
	}
	
	// expect a space after a comma
	static private Collection compileCollection (ArrayList<String> as) {
		ArrayList<ArrayList<String>> parsed = new ArrayList<ArrayList<String>>();
		ArrayList<String> inner = new ArrayList<String>();
		int openedParenthesis = 0;
		
		for (String s: as) {
			openedParenthesis += getParenthesisDifferential(s);
			if (openedParenthesis == 1 && s.endsWith(",")) {
				inner.add(s.substring(0, s.length()-1));
				parsed.add(inner);
				inner = new ArrayList<String>();
			} else {
				inner.add(s);
			}
		}
		parsed.add(inner);
		return compileCollectionParsed(parsed);
	}
	
	static private Collection compileCollectionParsed (ArrayList<ArrayList<String>> aas) {
		// Remove the collection header
		String[] firstelement = aas.get(0).get(0).split("\\(", 2);
		aas.get(0).set(0, firstelement[1]);
		
		// Remove last element closing parenthesis
		ArrayList<String> lastAs = aas.get(aas.size()-1);
		String lastelement = lastAs.get(lastAs.size()-1);
		lastAs.set(lastAs.size()-1, lastelement.substring(0, lastelement.length()-1));
		
		Collection coll = new Collection();
		if (firstelement[0].equals("\\cartprod")) coll.setCartesian(true);
		for (ArrayList<String> as: aas) coll.addTerm(compileTerms(as));
		return coll;
	}
	static private boolean isSurroundedByParenthesis(ArrayList<String> seq) {
		if (seq.get(0).charAt(0) != '(') return false;
		int openedParenthesis = 0;
		for (int i=0; i<seq.size(); i++) {
			char[] x = seq.get(i).trim().toCharArray();
			for (int j=0; j<x.length; j++) {
				if (x[j] == '(') openedParenthesis++;
				if (x[j] == ')') {
					openedParenthesis--;
					if (openedParenthesis == 0) return (i==(seq.size()-1)) && (j==x.length-1);
					if (openedParenthesis < 0) System.out.println("[FATAL] Error in parenthesis comprehension.");
				}
			}
		}
		System.out.println("[FATAL] Error in parenthesis comprehension.");
		return false;		
	}
	
	static private int getParenthesisDifferential(String x) {
		int result = 0;
		for (char c: x.toCharArray()) {
			if (c == '(') result++;
			if (c == ')') result--;
		}
		return result;
	}
	
	
	/*
	 * Demonstration territory
	 */
	static public ArrayList<Statement> extractDiffArray(Term tthm, Term tprop) throws ExceptionTheoremNotApplicable {
		ArrayList<Statement> result = new ArrayList<Statement>();
		
		Disp disp = tthm.getDisposition();
		if (disp == Disp.F) {
			// Following case implies no difference; no need to check.
			if (tprop.isShallow() && tprop.equals(tthm)) return result;
				
			
			// Can't overload a thm variable (thm.a can't be both equal to (x>0) and (x=0))
			for (Statement st: result) {
				if (st.lside.equals(tthm) && !st.rside.equals(tprop)) throw new ExceptionTheoremNotApplicable();
			}
			
			result.add(new Statement(new Link(":="), tthm, tprop));
		}
		else if (disp == Disp.C) {
			result.addAll(Collection.extractDiffArray(tthm, tprop));
		}
		else {
			if (disp != tprop.getDisposition()) throw new ExceptionTheoremNotApplicable();
			if (disp == Disp.OT) {
				assertSameOperator(tthm, tprop, 0);
				result.addAll(extractDiffArray(tthm.get(1), tprop.get(1)));
			} else if (disp == Disp.TOT) {
				assertSameOperator(tthm, tprop, 1);
				result.addAll(extractDiffArray(tthm.get(0), tprop.get(0)));
				result.addAll(extractDiffArray(tthm.get(2), tprop.get(2)));
			} else if (disp == Disp.QTT) {
				assertSameOperator(tthm, tprop, 0);
				result.addAll(extractDiffArray(tthm.get(1), tprop.get(1)));
				result.addAll(extractDiffArray(tthm.get(2), tprop.get(2)));
			} else throw new ExceptionTheoremNotApplicable();
		}
		return result;
	}	
	
	static private void assertSameOperator (Term a, Term b, int pos) throws ExceptionTheoremNotApplicable {
		if (!a.get(pos).equalsString(b.get(pos).s)) throw new ExceptionTheoremNotApplicable();
	}
	
	static class DifferencesLedger {
		
		private Term t1, t2;
		private boolean trivial;
		public ArrayList<Statement> diffs;
		
		public DifferencesLedger(Term t1, Term t2) {
			diffs = new ArrayList<Statement>();
			this.t1 = t1;
			this.t2 = t2;
			trivial = false;
		}
		public void addDifference(Statement st) {
			for (Statement diff: diffs) {
				if (diff.equals(st)) return;
			}
			diffs.add(st);
		}
		public void extractDiff(Link link) {
			if (Term.extractDiffInner(t1, t2, link, this)) trivial = true;
		}
		// TODO make use of it
		public boolean isTrivial() {
			return trivial;
		}
	}
	
	/*
	 * If our link is "=", then we need to prove that a = b or that f(a) = f(b)
	 * If our link is propositional, we need to prove that a = b, f(a) = f(b) (( when different names mean the same thing ))
	 *     or that a <=> b (( since  (a <==> b) <==> (a v c <==> a v b)  ))
	 *     idem for f(a) <=> f(b)
	 */
	static public ArrayList<Statement> extractDiff(Term t1, Term t2, Link link) throws ExceptionTrivialEquality {
		DifferencesLedger dlg = new DifferencesLedger(t1, t2);
		
		if (link.equals(Op.eq)) {
			dlg.extractDiff(link);
		} else if (link.equals(Op.equiv)) {
			dlg.extractDiff(new Link(Op.eq));
			dlg.extractDiff(link);
		} else if (link.equals(Op.le)) {
			dlg.extractDiff(new Link(Op.eq));
			dlg.extractDiff(new Link(Op.lt));
			dlg.extractDiff(link);
		} else if (link.equals(Op.ge)) {
			dlg.extractDiff(new Link(Op.eq));
			dlg.extractDiff(new Link(Op.gt));
			dlg.extractDiff(link);
		} else if (link.equals(Op.ineq)) {
			dlg.extractDiff(new Link(Op.gt));
			dlg.extractDiff(new Link(Op.lt));
			dlg.extractDiff(link);
		} else if (link.equals(Op.then)) {
			dlg.extractDiff(new Link(Op.eq));
			dlg.extractDiff(new Link(Op.equiv));
			dlg.extractDiff(link);
		} else {
			dlg.extractDiff(link);
		}
		
		if (dlg.isTrivial()) throw new ExceptionTrivialEquality();
		Collections.reverse(dlg.diffs);
		return dlg.diffs;
	}
	
	static boolean extractDiffInner(Term t1, Term t2, Link clink, DifferencesLedger dL) {

		Statement wholestatement = new Statement(clink, t1, t2);
		dL.addDifference(wholestatement);
		
		Disp d1 = t1.getDisposition();
		Disp d2 = t2.getDisposition();

		if (d1 != d2) return false;
		else if (t1.equals(t2)) return true;
		else {
			if (d1 == Disp.F) return false;
			if (d1 == Disp.C) return Collection.extractDiffInner(t1, t2, clink, dL);
			Term p10 = t1.get(0); Term p11 = t1.get(1);
			Term p20 = t2.get(0); Term p21 = t2.get(1);
			
			boolean eq0 = p10.equals(p20);
			boolean eq1 = p11.equals(p21);
			if (d1 == Disp.OT) {
				if (eq0) return extractDiffInnerWithReversing(p11, p21, clink, dL, p10.s);
				if (eq1) return false; // No sympathy for operator synonyms
				return false;
			} else {
				Term p12 = t1.get(2); Term p22 = t2.get(2);
				boolean eq2 = p12.equals(p22);
				
				if (d1 == Disp.TOT) {
					if (!eq1) return false;
					if (eq0) return extractDiffInnerWithReversing(p12, p22, clink, dL, p11.s);
					if (eq2) return extractDiffInnerWithReversing(p10, p20, clink, dL, p11.s);
				} else if (d1 == Disp.QTT) {
					if (!eq0) return false;
					if (eq1) return extractDiffInner(p12, p22, clink, dL);
					if (eq2) return extractDiffInner(p11, p21, clink, dL);
				}
				return false;
			}
		}
	}
	
	static private boolean extractDiffInnerWithReversing(Term a, Term b, Link clink, DifferencesLedger dL, String opstr) {
		Operator op = Op.getOperator(opstr);
		if (op.isReversing()) return extractDiffInner(b, a, clink, dL);
		else return extractDiffInner(a, b, clink, dL);
	}


	
	static public Permutations permute(Term t) {
		Permutations perm = new Permutations();
		Disp disp = t.getDisposition();
		if (disp == Disp.F) {
			perm.add(t);
		} 
		else if (disp == Disp.TOT) {
			Operator op = (Operator) (t.get(1));
			boolean commutative = op.isCommutative();
			for (Term permta: permute(t.get(0)).vs) {
			for (Term permtb: permute(t.get(2)).vs) {
				perm.add(glueTerms(new Term[]{permta, op, permtb}));
				if (commutative) perm.add(glueTerms(new Term[]{permtb, op, permta}));
			}
			}
		}
		else if (disp == Disp.OT) {
			for (Term permt: permute(t.get(1)).vs) {
				perm.add(glueTerms(new Term[]{t.get(0), permt}));
			}
		}
		else if (disp == Disp.QTT) {
			for (Term permc: permute(t.get(1)).vs) {
			for (Term permp: permute(t.get(2)).vs) {
				perm.add(glueTerms(new Term[]{t.get(0), permc, permp}));
			}
			}
		}
		else if (disp == Disp.SET) {
			
		}
		else if (disp == Disp.C) {
			return Collection.permute((Collection) t);
		}
		return perm;
	}
	
	
	static public class ExceptionTheoremNotApplicable extends Exception {};
	static public class ExceptionTrivialEquality extends Exception {};
	
	static public class Permutations {
		
		public ArrayList<Term> vs;
		public Permutations() {
			vs = new ArrayList<Term>();
		}
		public void add(Term t) {
			vs.add(t);
		}
	}
	
}

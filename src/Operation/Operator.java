package Operation;

import Elements.Term;

public class Operator extends Term {

	
	public String s;
	public Operator (String s) {
		this.s = s;
	}
	
	public boolean equals (Operator other) {
		return this.s.equals(other.s);
	}
	public Operator copy () {
		return new Operator(s);
	}
	
	
	private boolean isPartOfList(Operator[] opSet) {
		if (s==null) return false;
		for (Operator op: opSet) {
			if (s.equals(op)) return true;
		}
		return false;
	}
	
	public boolean isQuantifier() {
		Operator[] opSet = new Operator[]{Op.exists, Op.forall};
		return isPartOfList(opSet);
	}
	
	public boolean isSet() {
		return s.equals("\\set");
	}
	
	public boolean isUnary() {
		Operator[] opSet = new Operator[]{Op.not, Op.minus};
		return isPartOfList(opSet);
	}
	
	public boolean isBinary() {
		Operator[] opSet = new Operator[]{Op.or, Op.and, Op.implies, Op.eq, Op.gt, Op.lt, Op.ge, Op.le, Op.ineq, Op.mult, Op.plus, Op.minus,
										  Op.intersection, Op.union, Op.subset, Op.psubset, Op.in, Op.notin};
		return isPartOfList(opSet);
	}
	
	public boolean isOperator() {
		return isUnary() || isBinary() || isQuantifier();
	}
	
	public boolean isCommutative() {
		Operator[] opSet = new Operator[]{Op.or, Op.and, Op.eq, Op.ineq, Op.plus, Op.mult, Op.equiv, Op.intersection, Op.union};
		return isPartOfList(opSet);
	}
	
	public boolean isReversing() {
		Operator[] opSet = new Operator[]{Op.minus, Op.div};
		return isPartOfList(opSet);
	}

}

package Graphics;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.text.*;

import Elements.Assump;
import Elements.Justification;
import Core.Logging;
import Core.Logging.Arg;
import Core.StringOperations;
import Core.Theorem;
import Elements.Variable;

public class BotTextArea extends StringOperations {
	
	private SimpleAttributeSet sbold, svar, spale, sred, sboldred, set, sital;
	private Document doc;	
	private ArrayList<linkJustification> linkedJustifications;
	private TabList tablist;
	
	public BotTextArea(TabList tablist) {
		
		super();
		this.setContentType("text/plain; charset=UTF-16");
		
		doc = getStyledDocument();
		
		set = new SimpleAttributeSet();
		
		sbold = new SimpleAttributeSet();
		StyleConstants.setBold(sbold, true);
		
		sital = new SimpleAttributeSet();
		StyleConstants.setItalic(sital, true);
		
		svar = new SimpleAttributeSet();
		StyleConstants.setBold(svar, true);
		StyleConstants.setForeground(svar, Color.BLUE);
		
		spale = new SimpleAttributeSet();
		StyleConstants.setForeground(spale, Color.GRAY);
		
		sred = new SimpleAttributeSet();
		StyleConstants.setForeground(sred, Color.RED);
		
		sboldred = new SimpleAttributeSet();
		StyleConstants.setBold(sboldred, true);
		StyleConstants.setForeground(sboldred, Color.RED);
		
		linkedJustifications = new ArrayList<linkJustification>();
		this.tablist = tablist;
		
	}
	
	public void initializeText() {
		setText("");
	}
	
	/*    Controls the whole sequence of drawing, coloring, placing and indexing the demonstration    */
	public void drawTheorem(Theorem thm) throws BadLocationException {

		initializeText();
		if (thm == null) return;
		
		drawTheoremHeader(thm);
		drawTheoremBody(thm);
				
	}
	
	/*    Draws the header, i.e. the name, statement and assumptions    */
	private void drawTheoremHeader(Theorem thm) throws BadLocationException {
		
		appendString("Theorem " + thm.name, sbold);
		
		String s = " (From packages ";
		s += join(thm.packages, ", ");
		
		if (thm.tags.length > 0) {
			s += " Related tags: ";
			s += join(thm.tags, ", ");
		}
		appendString(s + "):\n\n");
		
		String tab = "    Let ";
		for (Variable v: thm.variables) {
			if (v.fromheader) {
				appendString(tab);
				appendString(v.name, svar);
				appendString(v.toHeader());
				tab = "        ";
			}
		}
		appendString("\n");
		
		boolean areHypothesis = false;
		tab = "    Where:\n        ";
		for (Assump asmp: thm.assumptions) {
			if (asmp.IndexStamp.equals("hyp")) {
				appendString(tab + asmp.st.toString());
				tab = "\n        ";
				areHypothesis = true;
			}
		}
		if (areHypothesis) appendString("\n\n");
		
		appendString("    Then:\n        " + thm.statement.toString());
	}
	
	/*    Draws the body of the theorem, e.g. everything that comes under the header   */
	private void drawTheoremBody(Theorem thm) throws BadLocationException {
		if (thm.nlog.isEmpty()) return;
		appendString("\n\n\nDemonstration:\n\n", sbold);
		drawLogging(thm.nlog, 1);
	}
	
	/*    Uses recursion to produce meaningful logging.    
	 *    Only otherwise called by the Controlling sequence 'drawTheoremBody'    */
	private void drawLogging(Logging lg, int ntab) throws BadLocationException {
		
		if (lg.t.equals(Logging.type.cases)) {
			drawLoggingInitialCaseStatement(lg, ntab);
			for (Logging l: lg.v) {
				drawLogging(l, ntab+2);
			}
			/*
		} else if (lg.t.equals(Logging.type.specificCase)) {
			for (Logging innerlg: lg.v) {
				drawLogging(innerlg, ntab);
			}
			appendString("\n");*/
			
		} else if (lg.t.equals(Logging.type.statement)) {
			drawLoggingStatement(lg, ntab);
			appendString("\n");
		} else if (lg.t.equals(Logging.type.ground)){
			for (Logging l: lg.v) {
				drawLogging(l, ntab);
			}
			if (lg.solved) appendString("CQFD.", sbold);
			else {
				appendString("<Could not match theorem proposition with the proven assumptions>\n", sbold);
				for (Assump a: lg.thm.assumptions) {
					appendString(" -) " + a.st.toString() + "\n");
				}
			}
			appendString("\n");
		}
	}
	
	
	private void drawLoggingInitialCaseStatement(Logging lg, int ntab) throws BadLocationException {
		appendString(getTab(ntab-1) + "(" + lg.blocID + ")", sbold);
		appendString("    " + "Case where ");
		appendString(lg.caseStatement.toString());
		appendString(":\n\n");
	}
	
	/*
	private void drawLoggingCases(Statement st, int ntab) throws BadLocationException {
		appendString(getTab(ntab) + "Let " + st.toString() + ":\n\n");
	}*/
	
	private void drawLoggingStatement(Logging lg, int ntab) throws BadLocationException {
		
		int maxsize = computeMaxsize(lg.args);
		appendString(getTab(ntab-1) + "(" + lg.blocID + ")", sbold);
		
		Arg firstarg = lg.args.get(0);
		if (firstarg.vars != null) {
			drawLoggingLetstatement(firstarg, ntab);
			return;
		}
			
		String firstLine = "    " + firstarg.term.toString();
		appendString(firstLine + "\n");
		
		String tab = getTab(ntab) + "     ";
		
		for (int i=1; i<lg.args.size(); i++) {
			Logging.Arg arg = lg.args.get(i);
			String linkstr = arg.link.toString();
			String argterm = arg.term.toString();
			String paddingSpaces = repeat(" ", maxsize - len(argterm) + 7 - len(linkstr));
			
			if (arg.expl.isError()) {
				appendString(tab + linkstr + argterm, sred);
				appendString(paddingSpaces);
				appendString(arg.expl.toString() + "\n", sboldred);
			} else {
				appendString(tab + linkstr + argterm);
				appendString(paddingSpaces);
				linkedJustifications.add(new linkJustification(arg.expl, doc.getLength()));
				appendString(arg.expl.getName() + "\n", spale);
			}
		}
	}
	
	private void drawLoggingLetstatement(Arg firstarg, int ntab) throws BadLocationException {
		String tab = "    Let ";
		for (Variable v: firstarg.vars) {
			appendString(tab);
			appendString(v.name, svar);
			appendString(v.toHeader());
			tab = getTab(ntab) + "        ";
		}
		
	}

	private int computeMaxsize (ArrayList<Arg> args) {
		int maxsize = 0;
		for (int i=1; i<args.size(); i++) {
			int currentsize = len(args.get(i).term.toString());
			if (currentsize > maxsize) maxsize = currentsize;
		}
		return maxsize;
	}
	
	private void appendString(String s, SimpleAttributeSet set) throws BadLocationException {
		ArrayList<Styledsequence> list = parseVisualText(s);
		for (Styledsequence ss: list) {
			
			if (ss.style.equals("bold")) {
				if (set.equals(sred)) insertStringToDoc(ss.sequence, sboldred);
				else insertStringToDoc(ss.sequence, set);
			}
			else if (ss.style.equals("red")) insertStringToDoc(ss.sequence, set);
			else if (ss.style.equals("boldred")) insertStringToDoc(ss.sequence, set);
			else insertStringToDoc(ss.sequence, set);
		}
	}
	private void appendString(String s) throws BadLocationException {
		ArrayList<Styledsequence> list = parseVisualText(s);
		for (Styledsequence ss: list) {
			
			if (ss.style.equals("bold")) insertStringToDoc(ss.sequence, sbold);
			else if (ss.style.equals("ital")) insertStringToDoc(ss.sequence, sital);
			else if (ss.style.equals("red")) insertStringToDoc(ss.sequence, sred);
			else if (ss.style.equals("boldred")) insertStringToDoc(ss.sequence, sboldred);
			else insertStringToDoc(ss.sequence, set);
		}

	}
	
	
	private void insertStringToDoc (String s, SimpleAttributeSet set) throws BadLocationException {
		doc.insertString(doc.getLength(), s, set);
	}
	
	
	public boolean isManagingFocus(){ return false; }
	
	
	public void jTextPanelMouseClicked(MouseEvent evt) {                                        
	   try {
            int pt = viewToModel(evt.getPoint());
            int spt = Utilities.getWordStart(this, pt);
            int ept = Utilities.getWordEnd(this, pt);
            
            for (linkJustification p: linkedJustifications) {
            	if (p.pos == spt) {
            		setSelectionStart(spt);
                    setSelectionEnd(ept);
                    if (p.justification.isTheorem) {
                    	
                    	tablist.addnew(p.justification.thm);
                    }
                    tablist.repaint();
                    break;
            	}
            }
	    } catch(Exception e) {
	        e.printStackTrace();
	    }
	}
	
	
	private class linkJustification {
		
		Justification justification;
		int pos;
		
		private linkJustification(Justification j, int pos) {
			this.justification = j;
			this.pos = pos;
		}
	}
	
}

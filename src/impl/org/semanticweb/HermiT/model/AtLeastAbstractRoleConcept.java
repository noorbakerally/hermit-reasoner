// Copyright 2008 by Oxford University; see license.txt for details
package org.semanticweb.HermiT.model;

import org.semanticweb.HermiT.*;

/**
 * Represents at-least concept.
 */
public class AtLeastAbstractRoleConcept extends ExistentialConcept implements DLPredicate {
    private static final long serialVersionUID=4326267535193393030L;

    protected final int m_number;
    protected final Role m_onRole;
    protected final LiteralConcept m_toConcept;
    
    protected AtLeastAbstractRoleConcept(int number,Role onRole,LiteralConcept toConcept) {
        m_number=number;
        m_onRole=onRole;
        m_toConcept=toConcept;
    }
    public int getNumber() {
        return m_number;
    }
    public Role getOnRole() {
        return m_onRole;
    }
    public LiteralConcept getToConcept() {
        return m_toConcept;
    }
    public int getArity() {
        return 1;
    }
    public String toString(Namespaces namespaces) {
        return "atLeast("+m_number+' '+m_onRole.toString(namespaces)+' '+m_toConcept.toString(namespaces)+')';
    }
    protected Object readResolve() {
        return s_interningManager.intern(this);
    }

    protected static InterningManager<AtLeastAbstractRoleConcept> s_interningManager=new InterningManager<AtLeastAbstractRoleConcept>() {
        protected boolean equal(AtLeastAbstractRoleConcept object1,AtLeastAbstractRoleConcept object2) {
            return object1.m_number==object2.m_number && object1.m_onRole==object2.m_onRole && object1.m_toConcept==object2.m_toConcept;
        }
        protected int getHashCode(AtLeastAbstractRoleConcept object) {
            return (object.m_number*7+object.m_onRole.hashCode())*7+object.m_toConcept.hashCode();
        }
    };
    
    public static AtLeastAbstractRoleConcept create(int number,Role onRole,LiteralConcept toConcept) {
        return s_interningManager.intern(new AtLeastAbstractRoleConcept(number,onRole,toConcept));
    }
}

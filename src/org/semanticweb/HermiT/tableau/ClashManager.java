// Copyright 2008 by Oxford University; see license.txt for details
package org.semanticweb.HermiT.tableau;

import java.io.Serializable;

import org.semanticweb.HermiT.model.AtomicConcept;
import org.semanticweb.HermiT.model.AtomicNegationConcept;
import org.semanticweb.HermiT.model.AtomicRole;
import org.semanticweb.HermiT.model.Inequality;
import org.semanticweb.HermiT.model.LiteralConcept;
import org.semanticweb.HermiT.model.NegatedAtomicRole;
import org.semanticweb.HermiT.monitor.TableauMonitor;

/**
 * An instance of this class is notified by extension tables when tuples are added. This class then
 * detects whether the addition of a tuple caused a clash or not. Clashes are detected as soon as
 * offending assertions are added to the extensions (i.e., they do not depend on delta-new).
 * This is so for performance reasons: we want to detect a clash ASAP so that we can save
 * ourselves unnecessary work.
 */
public final class ClashManager implements Serializable {

    private static final long serialVersionUID = 3533809151139695892L;

    protected static final LiteralConcept NOT_RDFS_LITERAL=AtomicConcept.RDFS_LITERAL.getNegation();
    
    protected final ExtensionManager m_extensionManager;
    protected final ExtensionTable.Retrieval m_ternaryExtensionTableSearch01Bound;
    protected final TableauMonitor m_tableauMonitor;
    protected final Object[] m_binaryAuxiliaryTuple;
    protected final Object[] m_ternaryAuxiliaryTuple;
    protected final UnionDependencySet m_binaryUnionDependencySet;

    public ClashManager(Tableau tableau) {
        m_extensionManager=tableau.m_extensionManager;
        m_ternaryExtensionTableSearch01Bound=m_extensionManager.m_ternaryExtensionTable.createRetrieval(new boolean[] { true,true,false },ExtensionTable.View.TOTAL);
        m_tableauMonitor=tableau.m_tableauMonitor;
        m_binaryAuxiliaryTuple=new Object[2];
        m_ternaryAuxiliaryTuple=new Object[3];
        m_binaryUnionDependencySet=new UnionDependencySet(2);
    }
    public void tupleAdded(ExtensionTable extensionTable,Object[] tuple,DependencySet dependencySet,boolean isCore) {
        Object dlPredicateObject=tuple[0];
        Node node0=(Node)tuple[1];
        if (AtomicConcept.NOTHING.equals(dlPredicateObject) || NOT_RDFS_LITERAL.equals(dlPredicateObject) || (Inequality.INSTANCE.equals(dlPredicateObject) && tuple[1]==tuple[2])) {
            if (m_tableauMonitor!=null)
                m_tableauMonitor.clashDetectionStarted(tuple);
            m_extensionManager.setClash(dependencySet);
            if (m_tableauMonitor!=null)
                m_tableauMonitor.clashDetectionFinished(tuple);
        }
        else if ((dlPredicateObject instanceof AtomicConcept && node0.m_numberOfNegatedAtomicConcepts>0) || (dlPredicateObject instanceof AtomicNegationConcept && node0.m_numberOfPositiveAtomicConcepts>0)) {
            m_binaryAuxiliaryTuple[0]=((LiteralConcept)dlPredicateObject).getNegation();
            m_binaryAuxiliaryTuple[1]=node0;
            if (extensionTable.containsTuple(m_binaryAuxiliaryTuple)) {
                m_binaryUnionDependencySet.m_dependencySets[0]=dependencySet;
                m_binaryUnionDependencySet.m_dependencySets[1]=extensionTable.getDependencySet(m_binaryAuxiliaryTuple);
                if (m_tableauMonitor!=null)
                    m_tableauMonitor.clashDetectionStarted(tuple,m_binaryAuxiliaryTuple);
                m_extensionManager.setClash(m_binaryUnionDependencySet);
                if (m_tableauMonitor!=null)
                    m_tableauMonitor.clashDetectionFinished(tuple,m_binaryAuxiliaryTuple);
            }
        }
        else if ((dlPredicateObject instanceof AtomicRole && ((Node)tuple[1]).m_numberOfNegatedRoleAssertions>0) || (dlPredicateObject instanceof NegatedAtomicRole)) {
            Object searchPredicate;
            if (dlPredicateObject instanceof AtomicRole)
                searchPredicate=NegatedAtomicRole.create((AtomicRole)dlPredicateObject);
            else
                searchPredicate=((NegatedAtomicRole)dlPredicateObject).getNegatedAtomicRole();
            m_ternaryAuxiliaryTuple[0]=searchPredicate;
            m_ternaryAuxiliaryTuple[1]=node0;
            m_ternaryAuxiliaryTuple[2]=tuple[2];
            if (extensionTable.containsTuple(m_ternaryAuxiliaryTuple)) {
                m_binaryUnionDependencySet.m_dependencySets[0]=dependencySet;
                m_binaryUnionDependencySet.m_dependencySets[1]=extensionTable.getDependencySet(m_ternaryAuxiliaryTuple);
                if (m_tableauMonitor!=null)
                    m_tableauMonitor.clashDetectionStarted(tuple,m_ternaryAuxiliaryTuple);
                m_extensionManager.setClash(m_binaryUnionDependencySet);
                if (m_tableauMonitor!=null)
                    m_tableauMonitor.clashDetectionFinished(tuple,m_ternaryAuxiliaryTuple);
            }
            else if (!((Node)tuple[2]).getNodeType().isAbstract()) {
                // If the second node is not abstract (i.e., if it is concrete), then we may need to generate inequalities.
                m_ternaryAuxiliaryTuple[0]=Inequality.INSTANCE;
                m_ternaryAuxiliaryTuple[1]=tuple[2];
                m_binaryUnionDependencySet.m_dependencySets[0]=dependencySet;
                m_ternaryExtensionTableSearch01Bound.getBindingsBuffer()[0]=searchPredicate;
                m_ternaryExtensionTableSearch01Bound.getBindingsBuffer()[1]=tuple[1];
                m_ternaryExtensionTableSearch01Bound.open();
                Object[] tupleBuffer=m_ternaryExtensionTableSearch01Bound.getTupleBuffer();
                while (!m_ternaryExtensionTableSearch01Bound.afterLast()) {
                    assert !((Node)tupleBuffer[2]).getNodeType().isAbstract();
                    m_ternaryAuxiliaryTuple[2]=tupleBuffer[2];
                    m_binaryUnionDependencySet.m_dependencySets[1]=m_ternaryExtensionTableSearch01Bound.getDependencySet();
                    if (m_tableauMonitor!=null)
                        m_tableauMonitor.clashDetectionStarted(tuple,tupleBuffer);
                    // Warning: the following call is reentrant. That is, we might be currently processing
                    // an addition on the extension manager, during which we then add another tuple.
                    // In general, such calls do not work. The added tuple is, however, quite simple,
                    // so such reentrant calls are OK. In order to prevent the reentrancy check in
                    // ExtensionManager, we go directly to the ternary table.
                    m_extensionManager.m_ternaryExtensionTable.addTuple(m_ternaryAuxiliaryTuple,m_binaryUnionDependencySet,true);
                    if (m_tableauMonitor!=null)
                        m_tableauMonitor.clashDetectionFinished(tuple,tupleBuffer);
                    m_ternaryExtensionTableSearch01Bound.next();
                }
            }
        }
    }
}
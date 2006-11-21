/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").  
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright 2005 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */

/*
 * ident	"@(#)JarAnalyzer.java 1.1     05/11/11 SMI"
 */
package org.opensolaris.opengrok.analysis.executables;

import org.apache.lucene.analysis.*;
import java.io.*;
import org.opensolaris.opengrok.analysis.*;
import org.opensolaris.opengrok.analysis.plain.*;
import org.apache.lucene.document.*;
import java.util.zip.*;
import java.util.*;
import org.opensolaris.opengrok.analysis.FileAnalyzer.Genre;

/**
 * Analyzes JAR, WAR, EAR (Java Archive) files.
 * Created on September 22, 2005
 *
 * @author Chandan
 */

public class JarAnalyzer extends FileAnalyzer {
    /**
     * Creates a new instance of JarAnalyzer
     */
    static byte[] content;
    int len;
    PlainFullTokenizer plainfull;
    
    private LinkedList<String> defs;
    private LinkedList<String> refs;
    private StringBuilder fullText;
    private StringWriter xref;
    
    public static String[] suffixes = {
	"JAR", "WAR", "EAR"
    };
    
    public static String[] magics = {
	"PK\003\004"
    };
    
    public static Genre g = Genre.XREFABLE;
    public Genre getGenre() {
	return this.g;
    }
    
    private static Reader dummy = new StringReader("");
    public JarAnalyzer() {
	super();
	content = new byte[16*1024];
	plainfull = new PlainFullTokenizer(dummy);
    }
    
    public void analyze(Document doc, InputStream in) {
	JavaClassAnalyzer jca = new JavaClassAnalyzer();
	defs = new LinkedList<String>();
	refs = new LinkedList<String>();
	fullText = new StringBuilder();
	xref = new StringWriter();
	try {
	    ZipInputStream zis = new ZipInputStream(in);
	    ZipEntry entry;
	    byte buf[] = new byte[1024];
	    while ((entry = zis.getNextEntry()) != null) {
		String ename = entry.getName();
		xref.write("<br/><b>"+ ename + "</b>");
		fullText.append(ename);
		fullText.append('\n');
		int len = 0;
		if (ename.endsWith(".class")) {
		    BufferedInputStream bif = new BufferedInputStream(zis);
		    int r;
		    while((r = bif.read(buf)) > 0) {
			if( len + r > content.length) {
			    byte[] content2 = new byte[content.length*2];
			    System.arraycopy(content, 0, content2, 0, len);
			    content = content2;
			}
			System.arraycopy(buf, 0, content, len, r);
			len += r;
		    }
		    jca.analyze(doc, new ByteArrayInputStream(content));
		    doc.removeField("defs");
		    doc.removeField("refs");
    		    doc.removeField("full");
		    defs.addAll(jca.getDefs());
		    refs.addAll(jca.getRefs());
		    fullText.append(jca.getFull());
		    xref.write("<pre>");
		    jca.writeXref(xref);
		    xref.write("</pre>");
		} else {
		    
		}
	    }
	    doc.add(Field.Text("full", new TagFilter(new StringReader(fullText.toString()))));
	    if(defs.size() > 0) {
		doc.add(Field.Text("defs",dummy));
	    }
	    if(refs.size() > 0) {
		doc.add(Field.Text("refs",dummy));
	    }
	} catch (IOException e) {
	    System.err.println(e);
	    e.printStackTrace();
	}
    }
    
    public TokenStream tokenStream(String fieldName, Reader reader) {
	if("defs".equals(fieldName)) {
	    return new List2TokenStream(defs);
	} else if ( "refs".equals(fieldName)) {
	    return new List2TokenStream(refs);
	} else if ("full".equals(fieldName)) {
	    return new PlainFullTokenizer(reader);
	}
	return super.tokenStream(fieldName, reader);
    }
    
    /**
     * Write a cross referenced HTML file.
     * @param out Writer to write HTML cross-reference
     */
    public void writeXref(Writer out) throws IOException {
	out.write(xref.toString());
    }
}
/*
 * 
 * $Revision$ $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.services.fieldquery;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom2.Element;
import org.mycore.common.MCRCalendar;
import org.mycore.common.MCRException;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.parsers.bool.MCRAndCondition;
import org.mycore.parsers.bool.MCRBooleanClauseParser;
import org.mycore.parsers.bool.MCRCondition;
import org.mycore.parsers.bool.MCRNotCondition;
import org.mycore.parsers.bool.MCROrCondition;
import org.mycore.parsers.bool.MCRParseException;
import org.mycore.parsers.bool.MCRSetCondition;

import com.ibm.icu.util.GregorianCalendar;

/**
 * Parses query conditions for use in MCRSearcher.
 * 
 * @see MCRSearcher
 * 
 * @author Frank Lützenkirchen
 */
public class MCRQueryParser extends MCRBooleanClauseParser {

    private final static Logger LOGGER = Logger.getLogger(MCRQueryParser.class);

    /**
     * Parses XML element containing a simple query condition
     * 
     * @param e
     *            the 'condition' element
     * @return the parsed MCRQueryCondition object
     */
    @Override
    protected MCRCondition<Object> parseSimpleCondition(Element e) throws MCRParseException {
        String name = e.getName();

        if (!name.equals("condition")) {
            throw new MCRParseException("Not a valid <" + name + ">");
        }

        String field = e.getAttributeValue("field");
        String opera = e.getAttributeValue("operator");
        String value = e.getAttributeValue("value");

        return buildConditions(field, opera, value);
    }

    /**
     * Builds a new MCRCondition from parsed elements
     * 
     * @param field
     *            one or more field names, separated by comma
     * @param oper
     *            the condition operator
     * @param value
     *            the condition value
     * @return
     */
    private MCRCondition<Object> buildConditions(String field, String oper, String value) {
        if (field.contains(",")) { // Multiple fields in one condition, combine with OR
            StringTokenizer st = new StringTokenizer(field, ", ");
            MCROrCondition<Object> oc = new MCROrCondition<Object>();
            while (st.hasMoreTokens()) {
                oc.addChild(buildConditions(st.nextToken(), oper, value));
            }
            return oc;
        } else if (field.contains("-")) { // date and MCRMetaHistoryDate condition von-bis
            StringTokenizer st = new StringTokenizer(field, "- ");
            String fieldFrom = st.nextToken();
            String fieldTo = st.nextToken();
            if (oper.equals("=")) {
                // von-bis = x --> (von <= x) AND (bis >= x)
                MCRAndCondition<Object> ac = new MCRAndCondition<Object>();
                ac.addChild(buildCondition(fieldFrom, "<=", value, true));
                ac.addChild(buildCondition(fieldTo, ">=", value, true));
                return ac;
            } else if (oper.contains("<")) {
                return buildCondition(fieldFrom, oper, value, true);
            } else {
                // oper.contains( ">" )
                return buildCondition(fieldTo, oper, value, true);
            }
        } else {
            return buildCondition(field, oper, value, false);
        }
    }

    /**
     * Builds a new MCRQueryCondition
     * 
     * @param field
     *            the name of the search field
     * @param oper
     *            the condition operator
     * @param value
     *            the condition value
     * @param vonbis
     *            is a 'from to' query, used for date and MetaHistoryDate
     * @return
     */
    private MCRQueryCondition buildCondition(String field, String oper, String value, boolean vonbis) {
        try {
            if (vonbis) {
                value = normalizeHistoryDate(oper, value);
            }
        } catch (MCRConfigurationException mcrExc) {
            // ignore exception if field not exist:
            // this is ugly, but MCRFieldDef shouldn't appear here to hack MCRMetaHistoryDates
        }
        if ("TODAY".equals(value)) {
            value = getToday();
        }
        return new MCRQueryCondition(field, oper, value);
    }

    private String getToday() {
        GregorianCalendar cal = new GregorianCalendar();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return String.valueOf(day) + "." + String.valueOf(month) + "." + String.valueOf(year);
    }

    /** Pattern for MCRQueryConditions expressed as String */
    private static Pattern pattern = Pattern.compile("([^ \t\r\n]+)\\s+([^ \t\r\n]+)\\s+([^ \"\t\r\n]+|\"[^\"]*\")");

    /**
     * Parses a String containing a simple query condition, for example: (title
     * contains "Java") and (creatorID = "122132131")
     * 
     * @param s
     *            the condition as a String
     * @return the parsed MCRQueryCondition object
     */
    @Override
    protected MCRCondition<Object> parseSimpleCondition(String s) throws MCRParseException {
        Matcher m = pattern.matcher(s);

        if (!m.find()) {
            throw new MCRParseException("Not a valid condition: " + s);
        }

        String field = m.group(1);
        String operator = m.group(2);
        String value = m.group(3);

        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }

        return buildConditions(field, operator, value);
    }

    @Override
    public MCRCondition<Object> parse(Element condition) throws MCRParseException {
        MCRCondition<Object> cond = super.parse(condition);
        return normalizeCondition(cond);
    }

    @Override
    public MCRCondition<Object> parse(String s) throws MCRParseException {
        MCRCondition<Object> cond = super.parse(s);
        return normalizeCondition(cond);
    }

    /**
     * Normalizes a parsed query condition. AND/OR conditions that just have one
     * child will be replaced with that child. NOT(NOT(X)) will be normalized to X.
     * (A AND (b AND c)) will be normalized to (A AND B AND C), same for nested ORs.
     * AND/OR/NOT conditions with no child conditions will be removed.
     * Conditions that use the operator "contains" will be splitted into multiple 
     * simpler conditions if the condition value contains phrases surrounded 
     * by '...' or wildcard search with * or ?.
     */
    public static MCRCondition<Object> normalizeCondition(MCRCondition<Object> cond) {
        if (cond == null) {
            return null;
        } else if (cond instanceof MCRSetCondition) {
            MCRSetCondition<Object> sc = (MCRSetCondition<Object>) cond;
            List<MCRCondition<Object>> children = sc.getChildren();
            sc = sc instanceof MCRAndCondition ? new MCRAndCondition<Object>() : new MCROrCondition<Object>();
            for (MCRCondition<Object> child : children) {
                child = normalizeCondition(child);
                if (child == null) {
                } else if (child instanceof MCRSetCondition
                    && sc.getOperator().equals(((MCRSetCondition) child).getOperator())) {
                    // Replace (a AND (b AND c)) with (a AND b AND c), same for OR
                    sc.addAll(((MCRSetCondition<Object>) child).getChildren());
                } else {
                    sc.addChild(child);
                }
            }
            children = sc.getChildren();
            if (children.size() == 0) {
                return null; // Completely remove empty AND condition
            } else if (children.size() == 1) {
                return children.get(0); // Replace AND with just one child
            } else {
                return sc;
            }
        } else if (cond instanceof MCRNotCondition) {
            MCRNotCondition<Object> nc = (MCRNotCondition<Object>) cond;
            MCRCondition<Object> child = normalizeCondition(nc.getChild());
            if (child == null) {
                return null; // Remove empty NOT
            } else if (child instanceof MCRNotCondition) {
                return normalizeCondition(((MCRNotCondition<Object>) child).getChild());
            } else {
                return new MCRNotCondition<Object>(child);
            }
        } else if (cond instanceof MCRQueryCondition) {
            MCRQueryCondition qc = (MCRQueryCondition) cond;

            if (!qc.getOperator().equals("contains")) {
                return qc;
            }

            // Normalize value when contains operator is used
            List<String> values = new ArrayList<String>();

            String phrase = null;
            StringTokenizer st = new StringTokenizer(qc.getValue(), " ");
            while (st.hasMoreTokens()) {
                String value = st.nextToken();
                if (phrase != null) // we are within phrase
                {
                    if (value.endsWith("'")) // end of phrase
                    {
                        value = phrase + " " + value;
                        values.add(value);
                        phrase = null;
                    } else // in middle of phrase
                    {
                        phrase = phrase + " " + value;
                    }
                } else if (value.startsWith("'")) // begin of phrase
                {
                    if (value.endsWith("'")) // one-word phrase
                    {
                        values.add(value.substring(1, value.length() - 1));
                    } else {
                        phrase = value;
                    }
                } else if (value.startsWith("-'")) // begin of NOT phrase
                {
                    if (value.endsWith("'")) // one-word phrase
                    {
                        values.add("-" + value.substring(2, value.length() - 1));
                    } else {
                        phrase = value;
                    }
                } else {
                    values.add(value);
                }
            }

            MCRAndCondition<Object> ac = new MCRAndCondition<Object>();
            for (String value : values) {
                if (value.startsWith("'")) {
                    ac.addChild(new MCRQueryCondition(qc.getFieldName(), "phrase", value.substring(1,
                        value.length() - 1)));
                } else if (value.startsWith("-'")) {
                    ac.addChild(new MCRNotCondition<Object>(new MCRQueryCondition(qc.getFieldName(), "phrase", value
                        .substring(2, value.length() - 1))));
                } else if (value.contains("*") || value.contains("?")) {
                    ac.addChild(new MCRQueryCondition(qc.getFieldName(), "like", value));
                } else if (value.startsWith("-")) // -word means "NOT word"
                {
                    MCRCondition<Object> subCond = new MCRQueryCondition(qc.getFieldName(), "contains",
                        value.substring(1));
                    ac.addChild(new MCRNotCondition<Object>(subCond));
                } else {
                    ac.addChild(new MCRQueryCondition(qc.getFieldName(), "contains", value));
                }
            }

            if (values.size() == 1) {
                return ac.getChildren().get(0);
            } else {
                return ac;
            }
        } else {
            return cond;
        }
    }

    /** Used for input validation in editor search form */
    public static boolean validateQueryExpression(String query) {
        try {
            MCRCondition<Object> cond = new MCRQueryParser().parse(query);
            return cond != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Normalizes MCRMetaHistoryDate values used in a query. If the
     * date is incomplete (for example, only the year given), it depends
     * on the search operator used, whether the upper (31th Dec of year)
     * or lower (1st Jan of year) bound is used.
     * 
     * @param operator the search operator, one of >, >=, <, <=
     * @param date the date to search for
     * @return the julian day number, as a String
     */
    private static String normalizeHistoryDate(String operator, String date) {
        GregorianCalendar cal = null;
        if (operator.equals(">")) {
            cal = MCRCalendar.getGregorianHistoryDate(date, true);
        }
        if (operator.equals("<")) {
            cal = MCRCalendar.getGregorianHistoryDate(date, false);
        }
        if (operator.equals(">=")) {
            cal = MCRCalendar.getGregorianHistoryDate(date, false);
        }
        if (operator.equals("<=")) {
            cal = MCRCalendar.getGregorianHistoryDate(date, true);
        }
        return String.valueOf(MCRCalendar.getJulianDayNumber(cal));
    }
}
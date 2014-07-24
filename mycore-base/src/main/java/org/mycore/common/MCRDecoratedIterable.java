/*
 * $Id$
 * $Revision: 5697 $ $Date: Jun 27, 2014 $
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

package org.mycore.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class acts as an {@link Iterable} on top of another instance.
 * 
 * Use this if you want to convert every element of a source into a different target object.
 * <br/>This example will print which file for a file name exists and which not:
 * <pre>
 * private void testExistence(List&lt;String&gt; fileNames) {
 *     for (File file : new MCRDecoratedIterable<String, File>(fileNames) {
 *         {@literal @}Override
 *         protected File getInstance(String source) {
 *             return new File(source);
 *         }
 *     }) {
 *         System.out.println(file.getAbsolutePath() + (file.exists() ? " exists." : " does not exist."));
 *     }
 * }
 * </pre>
 * @author Thomas Scheffler (yagee)
 * @since 2014.07
 */
public abstract class MCRDecoratedIterable<S, T> implements Iterable<T> {

    private final Iterable<S> source;

    /**
     * @param source every element of this will be transformed by {@link #getInstance(Object)}
     */
    public MCRDecoratedIterable(Iterable<S> source) {
        this.source = source;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            Iterator<S> baseIt = source.iterator();

            @Override
            public boolean hasNext() {
                return baseIt.hasNext();
            }

            @Override
            public T next() {
                return getInstance(baseIt.next());
            }

            @Override
            public void remove() {
                baseIt.remove();
            }

        };
    }

    /**
     * This is your transformation method to generate instances of T from S.
     */
    protected abstract T getInstance(S source);
}
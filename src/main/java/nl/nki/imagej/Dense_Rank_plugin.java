/*******************************************************************************
 * Copyright (c) 2019 R.Harkes NKI
 *    This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/
package nl.nki.imagej;

import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.IntegerType;

import org.scijava.app.StatusService;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import static java.lang.System.arraycopy;

import java.io.File;
import java.util.Iterator;

/**
 *
 */
@Plugin(type = Command.class, menuPath = "NKI>DenseRank")
public class Dense_Rank_plugin<T extends IntegerType<T>> implements Command {
	@Parameter
    private LogService log;
	
	@Parameter
    private StatusService statusService;
	
	@Parameter(label = "Select image", description = "the image field")
    private Img<T> img;

    @Override
    public void run() {
    	denseRank(img);
    }

    /**
     * This main function serves for development purposes.
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        // ask the user for a file to open
        final File file = ij.ui().chooseFile(null, "open");

        if (file != null) {
            // load the dataset
            final Dataset dataset = ij.scifio().datasetIO().open(file.getPath());

            // show the image
            ij.ui().show(dataset);

            // invoke the plugin
            ij.command().run(Dense_Rank_plugin.class, true);
        }
    }
    /**
    * @param img input image
    * @return integer array to convert the ranked image back
    * */ 
    public int[] denseRank(Img< T > img) {
    	IntegerType<T> temp = img.firstElement();
    	int values = 1 << temp.getBitsPerPixel();
    	//log.debug("Possible nr of values = "+values);
    	log.info("Possible nr of values = "+values);
    	boolean[] doesValueExist = new boolean[values];
        //go over all pixels to see what values exist
    	statusService.showStatus("ranking image (1/3)...");
        for (Iterator<T> iterator = img.iterator(); iterator.hasNext();) {
			IntegerType<T> t = iterator.next();
			doesValueExist[t.getInteger()]=true;
		}
        //create the unrank array and subtract array. 
        int subtract[] = new int[values];
        int unrankArrayFull[] = new int[values]; //unrank array unrankArray[data] --> original data
        int idx = 0;
        int subtractvalue = 0;
    	statusService.showStatus("ranking image (2/3)...");
        for (int i = 0; i < values; i++) {
            if (doesValueExist[i]) {
                subtract[i] = subtractvalue;
                unrankArrayFull[idx] = subtractvalue + idx;
                idx++;
            } else {
                subtractvalue++;
            }
        }
        //trim the unranking array
        int unrankArray[] = new int[idx];
        arraycopy(unrankArrayFull, 0, unrankArray, 0, idx);
        //rank data
    	statusService.showStatus("ranking image (3/3)...");
        for (Iterator<T> iterator = img.iterator(); iterator.hasNext();) {
			IntegerType<T> t = iterator.next();
			t.setInteger(t.getInteger()-subtract[t.getInteger()]);
		}
        statusService.showStatus("Finished!");
        return unrankArray;
    }
}
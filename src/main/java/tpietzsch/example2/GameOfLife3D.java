/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2018 SciView developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package tpietzsch.example2;

import bvv.util.Bvv;
import bvv.util.BvvFunctions;
import bvv.util.BvvStackSource;
import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.Sampler;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.Button;
import org.scijava.widget.NumberWidget;

/**
 * Conway's Game of Life&mdash;in 3D!
 *
 * @author Curtis Rueden
 */
@Plugin(type = Command.class)
public class GameOfLife3D extends InteractiveCommand {

    private static final int ALIVE = 255;
    private static final int DEAD = 128;

    private static final String SIX = "6-connected";
    private static final String EIGHTEEN = "18-connected";
    private static final String TWENTY_SIX = "26-connected";

    @Parameter(label = "Starvation threshold", min = "0", max = "26", persist = false)
    private int starvation = 5;

    @Parameter(label = "Birth threshold", min = "0", max = "26", persist = false)
    private int birth = 6;

    @Parameter(label = "Suffocation threshold", min = "0", max = "26", persist = false)
    private int suffocation = 9;

    @Parameter(choices = { SIX, EIGHTEEN, TWENTY_SIX }, persist = false)
    private String connectedness = TWENTY_SIX;

    @Parameter(label = "Initial saturation % when randomizing", min = "1", max = "99", style = NumberWidget.SCROLL_BAR_STYLE, persist = false)
    private int saturation = 10;

    @Parameter(callback = "iterate")
    private Button iterate;

    @Parameter(callback = "randomize")
    private Button randomize;

	private int w = 64, h = 64, d = 64;
    private Img<UnsignedByteType> field;
    private String name;
    private float[] voxelDims;

    /** Temporary buffer for use while recomputing the image. */
    private boolean[] bits = new boolean[w * h * d];

    /** Randomizes a new bit field. */
    public void randomize() {
        final Cursor<UnsignedByteType> cursor = field.localizingCursor();
        final double chance = saturation / 100d;
        while( cursor.hasNext() ) {
            final boolean alive = Math.random() <= chance;
            cursor.next().set( alive ? ALIVE : DEAD );
        }
        updateVolume();
    }

    /** Performs one iteration of the game. */
    public void iterate() {
		System.out.println( "GameOfLife3D.iterate" );

        final int connected;
        switch( connectedness ) {
        case SIX: connected = 6; break;
        case EIGHTEEN: connected = 18; break;
        default: connected = 26; break;
        }

        // compute the new image field
        final RandomAccess<UnsignedByteType> access = field.randomAccess();
        for( int z = 0; z < d; z++ ) {
            for( int y = 0; y < h; y++ ) {
                for( int x = 0; x < w; x++ ) {
                    final int i = z * w * h + y * w + x;
                    final int n = neighbors( access, x, y, z, connected );
                    access.setPosition( x, 0 );
                    access.setPosition( y, 1 );
                    access.setPosition( y, 2 );
                    if( alive( access ) ) {
                        // Living cell stays alive within (starvation, suffocation).
                        bits[i] = n > starvation && n < suffocation;
                    } else {
                        // New cell forms within [birth, suffocation).
                        bits[i] = n >= birth && n < suffocation;
                    }
                }
            }
        }

        // write the new bit field into the image
        final Cursor<UnsignedByteType> cursor = field.localizingCursor();
        while( cursor.hasNext() ) {
            cursor.fwd();
            final int x = cursor.getIntPosition( 0 );
            final int y = cursor.getIntPosition( 1 );
            final int z = cursor.getIntPosition( 2 );
            final boolean alive = bits[z * w * h + y * w + x];
			cursor.get().set( alive ? ALIVE : DEAD );
        }

        updateVolume();
    }

    @Override
    public void run() {
        field = ArrayImgs.unsignedBytes( w, h, d );
        randomize();

        //eventService.subscribe(this);
    }

    // -- Previewable methods --

    @Override
    public void preview() {
        // NB: Do nothing when parameters are tuned.
    }

    // -- Helper methods --

    private int neighbors( RandomAccess<UnsignedByteType> access, int x, int y, int z, int connected ) {
        int n = 0;
        // six-connected
        n += val( access, x - 1, y, z );
        n += val( access, x + 1, y, z );
        n += val( access, x, y - 1, z );
        n += val( access, x, y + 1, z );
        n += val( access, x, y, z - 1 );
        n += val( access, x, y, z + 1 );
        // eighteen-connected
        if( connected >= 18 ) {
            n += val( access, x - 1, y - 1, z );
            n += val( access, x + 1, y - 1, z );
            n += val( access, x - 1, y + 1, z );
            n += val( access, x + 1, y + 1, z );
            n += val( access, x - 1, y, z - 1 );
            n += val( access, x + 1, y, z - 1 );
            n += val( access, x - 1, y, z + 1 );
            n += val( access, x + 1, y, z + 1 );
            n += val( access, x, y - 1, z - 1 );
            n += val( access, x, y + 1, z - 1 );
            n += val( access, x, y - 1, z + 1 );
            n += val( access, x, y + 1, z + 1 );
        }
        // twenty-six-connected
        if( connected == 26 ) {
            n += val( access, x - 1, y - 1, z - 1 );
            n += val( access, x + 1, y - 1, z - 1 );
            n += val( access, x - 1, y + 1, z - 1 );
            n += val( access, x + 1, y + 1, z - 1 );
            n += val( access, x - 1, y - 1, z + 1 );
            n += val( access, x + 1, y - 1, z + 1 );
            n += val( access, x - 1, y + 1, z + 1 );
            n += val( access, x + 1, y + 1, z + 1 );
        }
        return n;
    }

    private int val( RandomAccess<UnsignedByteType> access, int x, int y, int z ) {
        if( x < 0 || x >= w || y < 0 || y >= h || z < 0 || z >= d ) return 0;
        access.setPosition( x, 0 );
        access.setPosition( y, 1 );
        access.setPosition( z, 2 );
        return alive( access ) ? 1 : 0;
    }

    private boolean alive( final Sampler<UnsignedByteType> access ) {
        return access.get().get() == ALIVE;
    }

    private long tick;

	BvvStackSource< ? > bvvSource = null;
	private void updateVolume() {
		if ( bvvSource == null )
			bvvSource = BvvFunctions.show( field, "Life Simulation", Bvv.options().maxAllowedStepInVoxels( 0 ) );
		else
			bvvSource.invalidate();
	}

	public static void main( String[] args )
	{
		final ImageJ ij = new ImageJ();
		ij.ui().showUI();
		ij.command().run( GameOfLife3D.class, true );
	}
}

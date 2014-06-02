/**
 * OpenSpace Android SDK Licence Terms
 *
 * The OpenSpace Android SDK is protected by © Crown copyright – Ordnance Survey 2013.[https://github.com/OrdnanceSurvey]
 *
 * All rights reserved (subject to the BSD licence terms as follows):.
 *
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * Neither the name of Ordnance Survey nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 *
 */
package uk.co.ordnancesurvey.android.maps;

import java.io.Closeable;
import java.io.IOException;

public abstract class OSTileSource implements Closeable
{
	private final String[] mProducts;
	
	OSTileSource(String[] productsOrNull) {
		mProducts = productsOrNull;
	}
	
	/**
	 * Blocking method to fetch a single tile.
	 *
	 * <b>Implementations must be thread-safe.</b>
	 *
	 * @param tile
	 * @return
	 */
	abstract byte[] dataForTile(MapTile tile);

	/**
	 * Is the tile loaded from the network? If so, we will take account of network reachability.
	 * @return
	 */
	abstract boolean isNetwork();

	/**
	 * Return true if the tiles should be loaded from the GL thread instead of a background thread.
	 * A reasonable rule of thumb is to return true if dataForTile() will return within ~10 ms.
	 *
	 * Regardless of what is returned, the implementation of {@link dataForTile()} must be thread-safe.
	 * @return
	 */
	abstract boolean isSynchronous();

	/** Whether results should be saved in the disk cache. The default is not to. */
	boolean shouldDiskCache() {
		return false;
	}
	

	boolean isProductSupported(String productCode)
	{
		if(mProducts == null)
		{
			return true;
		}

		for(int i = 0; i < mProducts.length; i++)
		{
			if(mProducts[i].equals(productCode))
			{
				return true;
			}
		}
		return false;
	}

    @Override
    public void close() throws IOException {
    }
}

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.mygdx.game.terrains;

import com.badlogic.gdx.utils.Array;
import com.mygdx.game.terrains.attributes.TerrainAttribute;
import com.mygdx.game.terrains.attributes.TerrainAttributes;

public class TerrainMaterial extends TerrainAttributes {
	private static int counter = 0;

	public String id;

	/** Create an empty material */
	public TerrainMaterial() {
		this("mtl" + (++counter));
	}

	/** Create an empty material */
	public TerrainMaterial(final String id) {
		this.id = id;
	}

	/** Create a material with the specified attributes */
	public TerrainMaterial(final TerrainAttribute... attributes) {
		this();
		set(attributes);
	}

	/** Create a material with the specified attributes */
	public TerrainMaterial(final String id, final TerrainAttribute... attributes) {
		this(id);
		set(attributes);
	}

	/** Create a material with the specified attributes */
	public TerrainMaterial(final Array<TerrainAttribute> attributes) {
		this();
		set(attributes);
	}

	/** Create a material with the specified attributes */
	public TerrainMaterial(final String id, final Array<TerrainAttribute> attributes) {
		this(id);
		set(attributes);
	}

	/** Create a material which is an exact copy of the specified material */
	public TerrainMaterial(final TerrainMaterial copyFrom) {
		this(copyFrom.id, copyFrom);
	}

	/** Create a material which is an exact copy of the specified material */
	public TerrainMaterial(final String id, final TerrainMaterial copyFrom) {
		this(id);
		for (TerrainAttribute attr : copyFrom)
			set(attr.copy());
	}

	/** Create a copy of this material */
	public TerrainMaterial copy () {
		return new TerrainMaterial(this);
	}
	
	@Override
	public int hashCode () {
		return super.hashCode() + 3 * id.hashCode();
	}
	
	@Override
	public boolean equals (Object other) {
		return (other instanceof TerrainMaterial) && ((other == this) || ((((TerrainMaterial)other).id.equals(id)) && super.equals(other)));
	}
}

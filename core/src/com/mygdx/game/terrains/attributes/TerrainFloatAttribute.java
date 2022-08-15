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

package com.mygdx.game.terrains.attributes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.NumberUtils;

public class TerrainFloatAttribute extends TerrainAttribute {
	public static final String MinSlopeAlias = "minSlope";
	public static final long MinSlope = register(MinSlopeAlias);

	public static TerrainFloatAttribute createMinSlope (float value) {
		return new TerrainFloatAttribute(MinSlope, value);
	}

	public float value;

	public TerrainFloatAttribute(long type) {
		super(type);
	}

	public TerrainFloatAttribute(long type, float value) {
		super(type);
		this.value = value;
	}

	@Override
	public TerrainAttribute copy () {
		return new TerrainFloatAttribute(type, value);
	}

	@Override
	public int hashCode () {
		int result = super.hashCode();
		result = 977 * result + NumberUtils.floatToRawIntBits(value);
		return result; 
	}
	
	@Override
	public int compareTo (TerrainAttribute o) {
		if (type != o.type) return (int)(type - o.type);
		final float v = ((TerrainFloatAttribute)o).value;
		return MathUtils.isEqual(value, v) ? 0 : value < v ? -1 : 1;
	}
}

package com.theotherhalf;

// which level the bars fill towards
public enum BarGoal
{
	SHOWN("Shown level"),
	REAL("Real level");

	private final String label;

	BarGoal(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}

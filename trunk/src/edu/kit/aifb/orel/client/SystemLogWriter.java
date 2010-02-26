package edu.kit.aifb.orel.client;

public class SystemLogWriter extends LogWriter {
	protected int errorThreshold;
	protected int outThreshold;
	
	public SystemLogWriter(int output, int errors) {
		errorThreshold = errors;
		outThreshold = output;
	}

	@Override
	public void print(String message, int errorlevel) {
		if (errorlevel < outThreshold) {
			return;
		} else if (errorlevel < errorThreshold) {
			System.out.print(message);
		} else {
			System.err.print(message);
		}
	}

	@Override
	public void println(String message, int errorlevel) {
		if (errorlevel < outThreshold) {
			return;
		} else if (errorlevel < errorThreshold) {
			System.out.println(message);
		} else {
			System.err.println(message);
		}
	}

}

package es.unican.istr.rama.render;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.Deflater;

public class PlantUMLEncoderService {

	public static final String DEFAULT_SERVER = "https://www.plantuml.com/plantuml/svg/";

	/**
	 * Encodes raw PlantUML text into the format expected by the PlantUML server.
	 *
	 * @param umlSource Raw PlantUML text
	 * @return Encoded string
	 */
	public String encode(String umlSource) {
		if (umlSource == null || umlSource.isBlank()) {
			throw new IllegalArgumentException("PlantUML source cannot be null or empty");
		}

		byte[] compressed = deflate(umlSource.getBytes(StandardCharsets.UTF_8));
		return encode64(compressed);
	}

	/**
	 * Generates a complete URL for the default PlantUML server using the encoded UML source.
	 *
	 * @param umlSource Raw PlantUML text
	 * @return Full URL to render the diagram on the default PlantUML server
	 */
	public String generateURL(String umlSource) {
		return generateURL(umlSource, DEFAULT_SERVER);
	}

	/**
	 * Generates a complete URL for the PlantUML server using the encoded UML source.
	 *
	 * @param umlSource Raw PlantUML text
	 * @param serverUrl Base URL of the PlantUML server
	 * @return Full URL to render the diagram on the specified PlantUML server
	 */
	public String generateURL(String umlSource, String serverUrl) {
		if (serverUrl == null || serverUrl.isBlank()) {
			throw new IllegalArgumentException("PlantUML server URL cannot be null or empty");
		}

		String base = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
		return base + encode(umlSource);
	}

	/**
	 * Compresses the input byte array using the DEFLATE algorithm.
	 *
	 * @param input Input byte array
	 * @return Compressed byte array
	 */
	private byte[] deflate(byte[] input) {
		Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
		deflater.setInput(input);
		deflater.finish();

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];

		while (!deflater.finished()) {
			int count = deflater.deflate(buffer);
			output.write(buffer, 0, count);
		}

		deflater.end();
		return output.toByteArray();
	}

	/**
	 * Encodes the input byte array into a custom Base64-like format used by PlantUML.
	 *
	 * @param data Input byte array
	 * @return Encoded string
	 */
	private String encode64(byte[] data) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < data.length; i += 3) {
			append3bytes(sb,
					i < data.length ? data[i] : 0,
					i + 1 < data.length ? data[i + 1] : 0,
					i + 2 < data.length ? data[i + 2] : 0);
		}

		return sb.toString();
	}

	/**
	 * Appends 3 bytes to the StringBuilder as 4 encoded characters.
	 *
	 * @param sb StringBuilder to append to
	 * @param b1 First byte
	 * @param b2 Second byte
	 * @param b3 Third byte
	 */
	private void append3bytes(StringBuilder sb, byte b1, byte b2, byte b3) {
		int c1 = b1 & 0xFF;
		int c2 = b2 & 0xFF;
		int c3 = b3 & 0xFF;

		int combined = (c1 << 16) | (c2 << 8) | c3;

		sb.append(encode6bit((combined >> 18) & 0x3F));
		sb.append(encode6bit((combined >> 12) & 0x3F));
		sb.append(encode6bit((combined >> 6) & 0x3F));
		sb.append(encode6bit(combined & 0x3F));
	}

	/**
	 * Encodes a 6-bit value into the corresponding character in the PlantUML encoding scheme.
	 *
	 * @param b 6-bit value (0-63)
	 * @return Encoded character
	 */
	private char encode6bit(int b) {
		if (b < 10) return (char) ('0' + b);
		b -= 10;
		if (b < 26) return (char) ('A' + b);
		b -= 26;
		if (b < 26) return (char) ('a' + b);
		b -= 26;
		if (b == 0) return '-';
		if (b == 1) return '_';
		return '?';
	}
}

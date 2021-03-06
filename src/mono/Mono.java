package mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;

/**
 * モノリスみたいなやつの試作品
 *
 * TODO
 * ・何も隠れてない
 *
 */
public class Mono {

	/** 使用するマークの種類 */
	public static List<String> USE_MARKS = new ArrayList<>(Arrays.asList("□", "○", "△"));
	/** 削除を意味するマーク番号*/
	public static int DELETE_INDEX = 99;

	/** 横幅 */
	private static int boardWidth = 10;
	/** 縦幅 */
	private static int boardHeight = 10;
	/** 現在のマス状況 */
	private static List<MarkData> list = new ArrayList<>();
	/** 処理済みリスト */
	private static List<String> processedList = new ArrayList<>();


	/**
	 * メイン処理
	 * @param args 引数は使ってない
	 */
	public static void main(String[] args) {

		System.out.println("start.");
		System.out.println();

		// 初期設定
		init();

		// ゲームボードをランダムで新規作成
		for (int i=1; i<=boardHeight; i++) {
			for (int j=1; j<=boardWidth; j++) {
				list.add(new MarkData(i, j, getRandomMarkIndex(), USE_MARKS));
			}
		}

		// 初回出力
		output();

		// 入力受付
		while(true){
			// ユーザ入力
			String[] split = getInput();

			// マス変更
			changeAction(Integer.valueOf(split[0]), Integer.valueOf(split[1]));

			// もう消せなくなったら終わりにする
			if (judgeNoMoreMove()) {
				System.out.println("これ以上消せません。終了します。");
				System.exit(0);
			}
		}
	}

	/**
	 * ゲームの初期設定を促す
	 * 縦行、横列、使用マーク種類を定義可能。
	 */
	private static void init() {
		System.out.println("デフォルト設定(10×10、使用マーク□,○,△)で始めます。OKであればy、設定を変更して遊ぶ場合はnを入力してください。");

		boolean setRow = false;
		boolean setColumn = false;
		boolean isErr = false;

		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String input = new String(in.readLine());

			 if (StringUtils.equals("n", StringUtils.lowerCase(input))) {
				 while (true) {
					 if (!setRow) {
						 if (!isErr) {
							 System.out.println("行数(縦)を入力してください。");
						 }
						 String row = in.readLine();
						 if (!checkNumber(row)) {
							 System.err.println("数値で入力してください。");
							 isErr = true;
							 continue;
						 }
						 boardHeight = Integer.parseInt(row);
						 setRow = true;
						 isErr = false;
					 }

					 if (!setColumn) {
						 if (!isErr) {
							 System.out.println("列数(横)を入力してください。");
						 }
						 String column = in.readLine();
						 if (!checkNumber(column)) {
							 System.err.println("半角数値で入力してください。");
							 isErr = true;
							 continue;
						 }
						 boardWidth = Integer.parseInt(column);
						 setColumn = true;
						 isErr = false;
					 }

					 if (!isErr) {
						 System.out.println("使用するマークを半角カンマ区切りで入力してください。");
					 }
					 String marks = in.readLine();
					 if (marks.indexOf(",") < 0) {
						 System.err.println("半角カンマ区切りで2種類以上入力してください。");
						 isErr = true;
						 continue;
					 }
					 String[] split = marks.split(",");
					 if (split.length < 2 || StringUtils.isBlank(split[0]) || StringUtils.isBlank(split[1])) {
						 System.err.println("半角カンマ区切りで2種類以上入力してください。");
						 isErr = true;
						 continue;
					 }
					 USE_MARKS = new ArrayList<String>(Arrays.asList(split));
					 isErr = false;
					 break;
				 }
			 }
		} catch (IOException e) {
			// do nothing;
		}
	}

	/**
	 * 標準入力から消すマスを指定させる
	 * 入力は「n-n」形式とし、フォーマットが異なる場合はエラーメッセージを出力して再度入力を促す
	 * Qを入力することでアプリケーションを終了させる
	 *
	 * @return 入力された縦、横の数値 [0]=縦 [1]=横
	 */
	private static String[] getInput() {

		 System.out.println("消したいマスを「縦-横」の形式で指定してください。たとえば左上のマスは1-1になります。終了する場合はQを入力してください。");

		while(true) {
			String input = StringUtils.EMPTY;
			try {
				 BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				 input = new String(in.readLine());

				 // 終了
				 if ("Q".equals(input)) {
					 System.out.println("終了します。");
					 System.exit(0);
				 }
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (StringUtils.isEmpty(input)) {
				System.err.println("入力してください。");
				continue;
			}

			if (input.indexOf("-") == -1) {
				System.err.println("半角ハイフン区切りで入力してください。");
				continue;
			}

			String[] split = input.split("-");

			if (!checkNumber(split[0])) {
				System.err.println("縦行は数値で入力してください。");
				continue;
			}

			if (!checkNumber(split[1])) {
				System.err.println("横列は数値で入力してください。");
				continue;
			}

			int row = Integer.parseInt(split[0]);
			int column = Integer.parseInt(split[1]);

			if (row < 0 || row > Mono.boardHeight) {
				System.err.println("範囲外の縦行が指定されています：" + row + "行");
				continue;
			}
			if (column < 0 || column > Mono.boardWidth) {
				System.err.println("範囲外の横列が指定されています：" + column + "列");
				continue;
			}

			MarkData mark = getMark(row, column);
			if (mark.getMarkIndex() == DELETE_INDEX) {
				System.err.println("既にそのマスは消し終わっています。");
				continue;
			}

			if (!checkAdjacent(mark)) {
				System.err.println("2マス以上繋がっていないと消せません。");
				continue;
			}

			return split;
		}
	}

	/**
	 * マス変更を呼び出し、実施後のボードを出力する
	 * @param row 入力された縦行
	 * @param column 入力された横列
	 */
	private static void changeAction(int row, int column) {

		// 指定されたマスの現在のマーク
		MarkData mark = getMark(row, column);
		processedList = new ArrayList<String>();
		change(mark);

		// 変更後のボードを再出力
		output();
	}

	/**
	 * 指定したmarkのマスが隣接マスのどこかと繋がっているか(同じマークかどうか)
	 * @param mark 基準マス
	 * @return 隣接マスのどこかと繋がっているか
	 */
	private static boolean checkAdjacent(MarkData mark) {

		int row = mark.getRow();
		int column = mark.getColumn();
		int markIndex = mark.getMarkIndex();

		// 基準マスの一個上
		if (row != 1) {
			if (checkSameMark(row-1, column, markIndex)) {
				return true;
			}
		}

		// 基準マスの一個下
		if (row != Mono.boardHeight) {
			if (checkSameMark(row+1, column, markIndex)) {
				return true;
			}
		}

		// 基準マスの一個左
		if (column != 1) {
			if (checkSameMark(row, column-1, markIndex)) {
				return true;
			}
		}

		// 基準マスの一個右
		if (column != Mono.boardWidth) {
			if (checkSameMark(row, column+1, markIndex)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 指定したrow-columnのマスのマーク番号が、markIndexと同一かどうか
	 * @param row 調査対象のマスの縦行
	 * @param column 調査対象のマスの横列
	 * @param markIndex 比較するマーク番号
	 * @return マーク番号が、markIndexと同一かどうか
	 */
	private static boolean checkSameMark(int row, int column, int markIndex) {
		MarkData mark = getMark(row, column);
		return markIndex == mark.getMarkIndex();
	}

	/**
	 * マス変更を行う。マス操作はそれぞれ以下のとおりに実施する。
	 *
	 * baseのマス:
	 *  削除する
	 *
	 * baseに隣接しているマスで、baseと同じマークだった場合:
	 *  削除し、更にその隣接マスを同様に処理するため、再帰でchangeをコールする
	 *
	 * baseに隣接しているマスで、baseと違うマークだった場合：
	 *  USE_MARKSで定義している順番にマークを変更する
	 *
	 * @param base 基準マス
	 */
	private static void change(MarkData base) {

		// 基準マス
		int row = base.getRow();
		int column = base.getColumn();
		int markIndex = base.getMarkIndex();
		String key = String.valueOf(row) + "_" + String.valueOf(column);
		if (!processedList.contains(key)) {
			processedList.add(key);
		}

		// 基準マスの一個上
		if (row != 1) {
			changeAdjacent(row-1, column, markIndex);
		}

		// 基準マスの一個下
		if (row != Mono.boardHeight) {
			changeAdjacent(row+1, column, markIndex);
		}

		// 基準マスの一個左
		if (column != 1) {
			changeAdjacent(row, column-1, markIndex);
		}

		// 基準マスの一個右
		if (column != Mono.boardWidth) {
			changeAdjacent(row, column+1, markIndex);
		}

		// 基準マスを消す
		base.deleteMark();
	}

	/**
	 * 隣接マスの処理を行う
	 *
	 * @param row 処理する隣接マスの縦
	 * @param column 処理する隣接マスの横
	 * @param markIndex 基準マスのマーク番号
	 */
	private static void changeAdjacent(int row, int column, int markIndex) {

		String key = String.valueOf(row) + "_" + String.valueOf(column);

		// 処理済みだったら無視
		if (!processedList.contains(key)) {
			MarkData data = getMark(row, column);
			// 基準マスのマークと違ったら変える
			if (data.getMarkIndex() != markIndex) {
				processedList.add(key);
				data.changeMark();
			} else {
				// 一緒だったら再帰処理
				change(data);
			}
		}
	}

	/**
	 * 縦、横を指定して、現在のボードからマス情報を取得する
	 * @param row 縦
	 * @param column 横
	 * @return マス情報
	 */
	private static MarkData getMark(int row, int column) {
		for (MarkData data : list) {
			if (data.getRow() == row && data.getColumn() == column) {
				return data;
			}
		}
		return null;
	}

	/**
	 * 現在のボードの情報を標準出力に出力する
	 */
	private static void output() {
		System.out.print("　");
		for (int i=1; i<=boardWidth; i++) {
			System.out.print(i);
			if (i < 10) {
				System.out.print(" ");
			}
		}
		System.out.println();

		for (MarkData data : list) {
			if (data.getColumn() == 1) {
				System.out.print(data.getRow());
				if (data.getRow() < 10) {
					System.out.print(" ");
				}
			}
			System.out.print(getMarkSymbol(data.getMarkIndex()));
			if (data.getColumn() == boardWidth) {
				System.out.println();
			}
		}
		System.out.println();
		System.out.println();
	}

	/**
	 * マーク番号からマークの文字を取得する
	 * @param index マーク番号（marksのindex）
	 * @return マークの文字
	 */
	private static String getMarkSymbol(int index) {

		// 消したマス
		if (index == DELETE_INDEX) {
			return "　";
		}
		return USE_MARKS.get(index);
	}

	/**
	 * 定義されたマークのうち、ランダムで一つ取得する（マーク番号を取得）
	 * @return ランダムで取得したマーク番号
	 */
	private static int getRandomMarkIndex() {
		int random = RandomUtils.nextInt(USE_MARKS.size());
		return random;
	}

	/**
	 * noMoreMoveかどうか判定する
	 * @return noMoreMoveかどうか
	 */
	private static boolean judgeNoMoreMove() {

		for (MarkData data : list) {
			// 繋がっているマスが1つでも見つかった時点でfalse
			if (DELETE_INDEX != data.getMarkIndex() && checkAdjacent(data)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 指定された文字列が数値として解釈可能かどうか
	 * @param number 文字列
	 * @return 文字列が数値として解釈可能かどうか
	 */
	private static boolean checkNumber(String number) {
		try {
			Integer.parseInt(number);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}
}

/**
 * マス一つの情報を管理するモデル
 *
 */
class MarkData {
	private int row;
	private int column;
	private int markIndex;

	public MarkData(int row, int column, int markIndex, List<String> marks) {
		this.row = row;
		this.column = column;
		this.markIndex = markIndex;
	}

	public int getColumn() {
		return column;
	}

	public int getRow() {
		return row;
	}

	public int getMarkIndex() {
		return markIndex;
	}

	public void setMarkIndex(int markIndex) {
		this.markIndex = markIndex;
	}

	/**
	 * マスが保持しているマーク番号を一つ進める
	 */
	public void changeMark() {
		// 一番最後のマークだったら最初に戻す
		if (markIndex == Mono.USE_MARKS.size() - 1) {
			markIndex = 0;
		} else if (markIndex == Mono.DELETE_INDEX) {
			// 削除済みのマスはそのまま何もしない
			// do nothing.
		} else {
			// それ以外は次のマークに変える
			markIndex++;
		}
	}

	/**
	 * マスを削除する
	 */
	public void deleteMark() {
		markIndex = Mono.DELETE_INDEX;
	}
}

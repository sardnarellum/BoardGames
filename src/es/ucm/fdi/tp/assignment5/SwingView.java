package es.ucm.fdi.tp.assignment5;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellRenderer;

import es.ucm.fdi.tp.assignment5.Main.PlayerMode;
import es.ucm.fdi.tp.assignment6.LogArea;
import es.ucm.fdi.tp.assignment6.TimeOutPlayer;
import es.ucm.fdi.tp.basecode.bgame.Utils;
import es.ucm.fdi.tp.basecode.bgame.control.Controller;
import es.ucm.fdi.tp.basecode.bgame.control.Player;
import es.ucm.fdi.tp.basecode.bgame.model.Board;
import es.ucm.fdi.tp.basecode.bgame.model.Game.State;
import es.ucm.fdi.tp.basecode.bgame.model.GameError;
import es.ucm.fdi.tp.basecode.bgame.model.GameObserver;
import es.ucm.fdi.tp.basecode.bgame.model.Observable;
import es.ucm.fdi.tp.basecode.bgame.model.Piece;

public abstract class SwingView extends JFrame implements GameObserver {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_TIMEOUT = 5;

	private Controller ctrl;
	private Piece localPiece;
	private Piece turn;
	private Player randPlayer;
	private TimeOutPlayer aiPlayer;
	private Board board;
	private List<Piece> pieces;
	private Map<Piece, Color> pieceColors;
	private Map<Piece, PlayerMode> playerModes;
	private LogArea messages;
	private JComboBox<String> playerColorsCB;
	private JComboBox<String> playerModesCB;
	private JComboBox<PlayerModeExt> modesCBox;
	private PlayerTableModel tmodel;
	private JButton randomBtn;
	private JButton automaticBtn;
	private JButton exitBtn;
	private JButton restartBtn;
	private JButton chooseColorBtn;
	private JButton setModeBtn;
	private boolean canMakeMove;

	public SwingView(Observable<GameObserver> g, Controller c, Piece lp, Player randPlayer, Player aiPlayer) {
		super();
		ctrl = c;
		localPiece = lp;
		playerModes = new HashMap<Piece, PlayerMode>();
		this.randPlayer = randPlayer;
		this.aiPlayer = aiPlayer != null ? new TimeOutPlayer(aiPlayer, DEFAULT_TIMEOUT) : null;
		canMakeMove = true;
		initGUI();
	}

	private void initGUI() {
		messages = new LogArea();
		messages.setEditable(false);
		messages.setLineWrap(true);
		messages.setWrapStyleWord(true);

		Border b = BorderFactory.createLineBorder(Color.BLACK);

		JPanel statusMessages = new JPanel();
		statusMessages.setLayout(new BorderLayout());
		statusMessages.setBorder(BorderFactory.createTitledBorder(b, "Status Messages"));
		JScrollPane messagePane = new JScrollPane(messages, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		statusMessages.setPreferredSize(new Dimension(200, 500));
		// messagePane.setPreferredSize(new Dimension(300, 300));
		statusMessages.add(messagePane, BorderLayout.CENTER);

		tmodel = new PlayerTableModel();
		JTable playerInfoTable = new JTable(tmodel) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
				Component comp = super.prepareRenderer(renderer, row, col);
				Color c = pieceColors.get(pieces.get(row));
				comp.setBackground(pieceColors.get(pieces.get(row)));

				if (c.getBlue() < 200 && c.getGreen() < 180 && c.getRed() < 160) {
					comp.setForeground(Color.white);
				} else {
					comp.setForeground(Color.black);
				}
				return comp;
			}
		};

		JPanel playerInfo = new JPanel();
		playerInfo.setLayout(new BorderLayout());
		playerInfo.setBorder(BorderFactory.createTitledBorder(b, "Player Information"));
		playerInfo.add(new JScrollPane(playerInfoTable), BorderLayout.CENTER);
		playerInfoTable.setFillsViewportHeight(true);

		JPanel colors = new JPanel(new FlowLayout(FlowLayout.LEFT));
		colors.setBorder(BorderFactory.createTitledBorder(b, "Piece Colors"));

		playerColorsCB = new JComboBox<String>();

		chooseColorBtn = new JButton("Choose Color");
		colors.add(playerColorsCB);
		colors.add(chooseColorBtn);

		chooseColorBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					Piece p = pieces.get(playerColorsCB.getSelectedIndex());
					ColorChooser c = new ColorChooser(new JFrame(), "Choose Line Color", pieceColors.get(p));
					if (c.getColor() != null) {
						pieceColors.put(p, c.getColor());
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								tmodel.refresh();
								redrawBoard();
							}
						});
					}
				} catch (Exception _e) {
					addMsg(_e.getMessage());
				}
			}
		});

		JPanel modes = new JPanel(new FlowLayout(FlowLayout.LEFT));
		modes.setBorder(BorderFactory.createTitledBorder(b, "Player Modes"));

		playerModesCB = new JComboBox<String>();

		modesCBox = new JComboBox<PlayerModeExt>();

		if (null != randPlayer || null != aiPlayer) {
			modesCBox.addItem(new PlayerModeExt(PlayerMode.MANUAL));

			if (null != randPlayer) {
				modesCBox.addItem(new PlayerModeExt(PlayerMode.RANDOM));
			}

			if (null != aiPlayer) {
				modesCBox.addItem(new PlayerModeExt(PlayerMode.AI));
			}
		}

		setModeBtn = new JButton("Set");
		setModeBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				final Piece p = localPiece == null ? pieces.get(playerModesCB.getSelectedIndex()) : localPiece;
				PlayerModeExt pme = (PlayerModeExt) modesCBox.getSelectedItem();
				PlayerMode m = pme.getPlayerMode();
				setPlayerMode(p, m);
			}

		});

		modes.add(playerModesCB);
		modes.add(modesCBox);
		modes.add(setModeBtn);

		JPanel autoOptions = new JPanel();
		autoOptions.setBorder(BorderFactory.createTitledBorder(b, "Automatic Moves"));

		randomBtn = new JButton(PlayerMode.RANDOM.getDesc());
		randomBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				decideMakeRandomMove();
			}
		});

		automaticBtn = new JButton(PlayerMode.AI.getDesc());
		automaticBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				decideMakeAutomaticMove();
			}
		});

		if (null != randPlayer) {
			autoOptions.add(randomBtn);
		}

		if (null != aiPlayer) {
			autoOptions.add(automaticBtn);
		}

		JLabel secondsLabel = new JLabel("seconds", JLabel.CENTER);
		secondsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JSlider timeoutSeconds = new JSlider(JSlider.HORIZONTAL, 0, 60, DEFAULT_TIMEOUT);
		timeoutSeconds.setMajorTickSpacing(10);
		timeoutSeconds.setMinorTickSpacing(1);
		timeoutSeconds.setPaintTicks(true);
		timeoutSeconds.setPaintLabels(true);

		timeoutSeconds.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting()) {
					setTimeOut(source.getValue());
				}
			}
		});

		JPanel timeoutOption = new JPanel();
		timeoutOption.setBorder(BorderFactory.createTitledBorder(b, "Timeout"));
		timeoutOption.setLayout(new BoxLayout(timeoutOption, BoxLayout.PAGE_AXIS));
		timeoutOption.add(secondsLabel);
		timeoutOption.add(timeoutSeconds);

		JPanel quitRestartPanel = new JPanel();

		exitBtn = new JButton("Exit");
		exitBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		restartBtn = new JButton("Restart");
		restartBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onRestart();
			}
		});

		quitRestartPanel.add(exitBtn);
		quitRestartPanel.add(restartBtn);

		JPanel dashboardPanel = new JPanel();
		dashboardPanel.setLayout(new BoxLayout(dashboardPanel, BoxLayout.Y_AXIS));
		dashboardPanel.add(statusMessages);
		dashboardPanel.add(playerInfo);
		dashboardPanel.add(colors);

		if (null != randPlayer || null != aiPlayer) {
			dashboardPanel.add(modes);
			dashboardPanel.add(autoOptions);
			dashboardPanel.add(timeoutOption);
		}

		dashboardPanel.add(quitRestartPanel);
		dashboardPanel.setPreferredSize(new Dimension(300, 600));

		JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
		mainPanel.add(dashboardPanel, BorderLayout.EAST);
		mainPanel.setOpaque(true);

		setContentPane(mainPanel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(900, 600);
		initBoardGUI();
		setManualControlEnabled(false);
		setExitRestartEnabled(false);
		setPlayerSettingsUIEnabled(false);
		setVisible(true);
		addMsg("Please wait until the game is loaded...");
	}

	private void setPlayerMode(Piece piece, PlayerMode mode) {
		if (!playerModes.get(piece).equals(mode)) {
			playerModes.put(piece, mode);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					tmodel.setMode(piece, mode);
					tmodel.refresh();
				}
			});
			if (canMakeMove) {
				if (turn.equals(piece)) {
					decideMakeRndOrAutoMove();
				}
			}
		}
	}

	private void setTimeOut(int seconds) {
		aiPlayer.setTimeOut(seconds);
	}

	final protected Piece getTurn() {
		return turn;
	}

	final protected Board getBoard() {
		return board;
	}

	final protected List<Piece> getPieces() {
		return pieces;
	}

	final protected Color getPieceColor(Piece p) {
		return pieceColors.get(p);
	}

	final protected PlayerMode getPlayerMode(Piece p) {
		return playerModes.get(p);
	}

	final protected Color setPieceColor(Piece p, Color c) {
		if (null == pieceColors) {
			pieceColors = new HashMap<Piece, Color>();
		}
		return pieceColors.put(p, c);
	}

	final protected void setBoardArea(JComponent c) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				getContentPane().add(c, BorderLayout.CENTER);
			}
		});
	}

	final protected void addMsg(String msg) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				messages.addLine(msg);
			}
		});
	}

	final protected void decideMakeManualMove(Player manualPlayer) {
		propagateMovement(manualPlayer);
	}

	private void decideMakeRndOrAutoMove() {
		switch (playerModes.get(turn)) {
		case AI:
			decideMakeAutomaticMove();
			break;
		case RANDOM:
			decideMakeRandomMove();
			break;
		default:
			break;
		}
	}

	private void decideMakeAutomaticMove() {
		propagateMovement(aiPlayer);
	}

	private void decideMakeRandomMove() {
		propagateMovement(randPlayer);
	}

	private void propagateMovement(final Player player) {
		try {
			if (null == player) {
				throw new GameError("The player parameter must be initialized.");
			}
			canMakeMove = false;
			setManualControlEnabled(false);
			setExitRestartEnabled(false);
			new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() throws Exception {
					ctrl.makeMove(player);
					return null;
				}
			}.execute();
		} catch (GameError e) {
			handleOnError(e.getMessage() + " turn is:" + turn);
		}
	}

	protected abstract void initBoardGUI();

	protected abstract void activateBoard();

	protected abstract void deActivateBoard();

	protected abstract void redrawBoard();

	@Override
	public void onGameStart(Board board, String gameDesc, List<Piece> pieces, Piece turn) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				handleOnGameStart(board, gameDesc, pieces);
				handleOnChangeTurn(turn);
			}
		});
	}

	@Override
	public void onGameOver(Board board, State state, Piece winner) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				handleOnGameOver(state, winner);
			}
		});
	}

	@Override
	public void onMoveStart(Board board, Piece turn) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				handleOnMoveStart();
			}
		});
	}

	@Override
	public void onMoveEnd(Board board, Piece turn, boolean success) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				handleOnMoveEnd(board, success);
			}
		});
	}

	@Override
	public void onChangeTurn(Board board, Piece turn) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				handleOnChangeTurn(turn);
			}
		});
	}

	@Override
	public void onError(String msg) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				handleOnError(msg);
			}
		});
	}

	protected void onRestart() {
		ctrl.restart();
	}

	private void handleOnGameStart(Board board, String gameDesc, List<Piece> pieces) {
		this.board = board;
		if (null != localPiece) {
			this.setTitle("Board Games: " + gameDesc + " (" + localPiece.toString() + ")");
		} else {
			this.setTitle("Board Games: " + gameDesc);
		}
		this.pieces = pieces;

		playerModesCB.removeAllItems();
		playerColorsCB.removeAllItems();

		Iterator<Color> ci = Utils.colorsGenerator();

		for (Piece p : pieces) {
			setPieceColor(p, ci.next());
			playerColorsCB.addItem(p.toString());

			if (null == localPiece) {
				playerModesCB.addItem(p.toString());
			}
			playerModes.put(p, PlayerMode.MANUAL);
			if (null != board.getPieceCount(p)) {
				tmodel.setRow(p, playerModes.get(p), board.getPieceCount(p));
			} else {
				tmodel.setRow(p, playerModes.get(p));
			}
		}

		if (null != localPiece) {
			playerModesCB.addItem(localPiece.toString());
		}

		tmodel.refresh();
		redrawBoard();
		addMsg(gameDesc + " is started");
		setManualControlEnabled(true);
		setExitRestartEnabled(true);
		setPlayerSettingsUIEnabled(true);
	}

	private void handleOnGameOver(State state, Piece winner) {
		setManualControlEnabled(false);
		setExitRestartEnabled(true);
		addMsg("GAME OVER");
		printState(state, winner);
	}

	protected void handleOnMoveStart() {
		setExitRestartEnabled(false);
		setManualControlEnabled(false);
	}

	protected void handleOnMoveEnd(Board board, boolean success) {
		if (success) {
			this.board = board;

			for (Piece p : pieces) {
				Integer c = board.getPieceCount(p);
				tmodel.setScore(p, c);
			}
			tmodel.refresh();
			redrawBoard();
		}
		
		canMakeMove = true;

		if (playerModes.get(turn) == PlayerMode.MANUAL) {
			setExitRestartEnabled(true);
			setManualControlEnabled(true);
		}
	}

	protected void handleOnChangeTurn(Piece turn) {
		this.turn = turn;

		if (null != localPiece) {
			if (localPiece.equals(turn)) {
				addMsg("The next player is " + turn + " (You)");
				if (playerModes.get(turn) == PlayerMode.MANUAL) {
					setManualControlEnabled(true);
					setExitRestartEnabled(true);
				} else {
					decideMakeRndOrAutoMove();
				}
			} else {
				setExitRestartEnabled(false);
				setManualControlEnabled(false);
				addMsg("The next player is " + turn);
			}
		} else {
			addMsg("The next player is " + turn);
			if (playerModes.get(turn) == PlayerMode.MANUAL) {
				setExitRestartEnabled(true);
				setManualControlEnabled(true);
			} else {
				decideMakeRndOrAutoMove();
			}
		}
	}

	protected void handleOnError(String msg) {
		addMsg("[Error] " + msg);
		if (null == localPiece || localPiece.equals(turn)) {
			canMakeMove = true;
			setPlayerMode(turn, PlayerMode.MANUAL);
			setManualControlEnabled(true);
			setExitRestartEnabled(true);
		}
	}

	private void printState(State state, Piece winner) {
		switch (state) {
		case Draw:
			addMsg("The Game is ended with Draw.");
			break;
		case InPlay:
			addMsg("The Game is in progress.");
			break;
		case Starting:
			addMsg("The Game is about to start.");
			break;
		case Stopped:
			addMsg("The Game is stopped");
			break;
		case Won:
			if (null != winner) {
				addMsg("The winner is: " + winner);
			}
			break;
		}
	}

	private void setPlayerSettingsUIEnabled(boolean enabled) {
		playerColorsCB.setEnabled(enabled);
		playerModesCB.setEnabled(enabled);
		modesCBox.setEnabled(enabled);
		chooseColorBtn.setEnabled(enabled);
		setModeBtn.setEnabled(enabled);
	}

	private void setManualControlEnabled(Boolean enabled) {
		automaticBtn.setEnabled(enabled);
		randomBtn.setEnabled(enabled);

		if (enabled) {
			activateBoard();
		} else {
			deActivateBoard();
		}
	}

	private void setExitRestartEnabled(Boolean enabled) {
		exitBtn.setEnabled(enabled);
		restartBtn.setEnabled(enabled);
	}
}

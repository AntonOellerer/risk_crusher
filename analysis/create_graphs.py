import glob
import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path

from log_file_type import LogFileType


def get_log_type(log_file_name):
    if 'OCCUPIED_TERRITORY_COUNT' in log_file_name:
        return LogFileType.TERRITORY_CNT
    elif 'TROOP_SIZE_EV' in log_file_name:
        return LogFileType.TROOP_SIZE
    elif 'CONTINENT_OCCUPATION_RATES' in log_file_name:
        return LogFileType.CONTINENT_OCCUPATION

    print("Unknown log file", log_file_name)
    return None


def handle_log_file(log_file):
    log_file_type = get_log_type(log_file)
    run_name = log_file.split("_")[1]
    Path(f"img_run_{run_name}/").mkdir(parents=True, exist_ok=True)

    if log_file_type == LogFileType.TERRITORY_CNT:
        handle_default_log(log_file, ['turn', 'player', 'total_territories', 'frontline_territories', 'backup_territories'])
    elif log_file_type == LogFileType.TROOP_SIZE:
        handle_default_log(log_file, ['turn', 'player', 'total_troops', 'frontline_troops', 'backup_troops'])
    elif log_file_type == LogFileType.CONTINENT_OCCUPATION:
        handle_player_log(log_file, ['turn', 'player', 'north_america', 'south_america', 'europe', 'africa', 'asia', 'australia'], 0)


def handle_default_log(log_file, col_names):
    run_name = log_file.split("_")[1]
    df = pd.read_csv(log_file, header=None)
    df.columns = col_names
    for col in df.columns[2:]:
        fig, ax = plt.subplots(figsize=(8, 6))
        ax.set_ylabel("Count")
        for label, sub_df in df.groupby(['player']):
            sub_df.plot(x='turn', y=col, ax=ax, label=f'player_{label}', title=col)
        fig.savefig(f'img_run_{run_name}/{col}.pdf')


def handle_player_log(log_file, col_names, player_id):
    run_name = log_file.split("_")[1]
    df = pd.read_csv(log_file, header=None)
    df.columns = col_names
    df = df[df.player == player_id]
    fig, ax = plt.subplots(figsize=(8, 6))
    ax.set_ylabel("Ratio")
    for col in df.columns[2:]:
        df.plot(x='turn', y=col, ax=ax, label=col, title=f"Occupation ratio for player {player_id}")
    fig.savefig(f'img_run_{run_name}/occupation_ratio.pdf')


if __name__ == '__main__':
    log_files = glob.glob('../*.log')
    for log_file in log_files:
        print(f"Analyzing {log_file} ...")
        handle_log_file(log_file)

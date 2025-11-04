import pandas as pd

# Load CSV
df = pd.read_csv('Raw_Transactions.csv')

# Get unique types
types = sorted(df['Type'].unique())

print(f'Total unique types: {len(types)}')
print('\n' + '='*50)
print('All Types in CSV Training Data:')
print('='*50 + '\n')

for i, t in enumerate(types, 1):
    print(f'{i:2d}. {t}')

print('\n' + '='*50)
print(f'These are the valid types that the recommendation model was trained on.')
print('Product types in your database must match these exactly (case-sensitive).')


import config

def listing(**k):
    return config.DB.select('unique_entities', **k)

def entity_listing(**k):
    return config.DB.select('entities', **k)

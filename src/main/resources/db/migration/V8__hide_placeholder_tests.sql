update tests
set status = 'DRAFT',
    updated_at = now()
where lower(title) in ('bssnzn')
   or lower(slug) in ('bssnzn');
